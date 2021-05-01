/*
 *     TunProxy is a proxy forwarding tool using Android's VPNService.
 *     Copyright (C) 2021 raise.isayan@gmail.com / Karsten Ohme
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package tun.proxy;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import javax.inject.Inject;

import tun.proxy.databinding.AppStateViewDataBinding;
import tun.proxy.di.DaggerWrapper;
import tun.proxy.event.HideAppEvent;
import tun.proxy.event.SingleLiveEvent;
import tun.proxy.model.AppState;
import tun.proxy.service.TunProxyVpnService;
import tun.proxy.view.AppStateViewModel;
import tun.utils.IPUtil;
import tun.proxy.service.SharedPrefService;

/**
 * Main activity of the app.
 *
 * @author <a href="mailto:raise.isayan@gmail.com">raise.isayan@gmail.com</a>
 */
public class MainActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final int REQUEST_VPN = 1;

    private EditText hostEditText;
    private final Handler statusHandler = new Handler();

    private TunProxyVpnService service;

    @Inject
    SingleLiveEvent<HideAppEvent> hideAppEventSingleLiveEvent;

    @Inject
    SingleLiveEvent<Exception> exceptionEventSingleLiveEvent;

    @Inject
    AppState appState;

    @Inject
    AppStateViewModel appStateViewModel;

    @Inject
    SharedPrefService sharedPrefService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerWrapper.getComponent(this).inject(this);

        AppStateViewDataBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.setAppState(appStateViewModel);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        hostEditText = findViewById(R.id.host);

        hideAppEventSingleLiveEvent.observe(this, new Observer<HideAppEvent>() {
            @Override
            public void onChanged(HideAppEvent hideAppEvent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.moveTaskToBack(true);
                    }
                });
            }
        });
        exceptionEventSingleLiveEvent.observe(this, new Observer<Exception>() {
            @Override
            public void onChanged(final Exception ex) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showError(ex);
                    }
                });
            }
        });
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String errorMsg = e.getMessage();
                androidx.appcompat.app.AlertDialog.Builder alertDialogBuilder = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                final androidx.appcompat.app.AlertDialog alertDialog = alertDialogBuilder
                        .setTitle(R.string.msg_exception)
                        .setMessage(errorMsg)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }).
                                setCancelable(false).create();

                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
        });
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(getClassLoader(), pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_activity_settings);
        item.setEnabled(!appState.isProxyRunning());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_activity_settings:
                Intent intent = new android.content.Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_show_about:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_about, getVersionName()))
                        .setMessage(R.string.app_disclaimer)
                        .show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    protected String getVersionName() {
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            return null;
        }

        try {
            return packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            TunProxyVpnService.ServiceBinder serviceBinder = (TunProxyVpnService.ServiceBinder) binder;
            service = serviceBinder.getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();

        statusHandler.post(statusRunnable);

        Intent intent = new Intent(this, TunProxyVpnService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final Runnable statusRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            statusHandler.post(statusRunnable);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        statusHandler.removeCallbacks(statusRunnable);
        unbindService(serviceConnection);
    }

    private void updateStatus() {
        if (service == null) {
            return;
        }
        if (appState.isProxyRunning()) {
            loadHostPort();
        }
    }

    public void stopVpn(View view) {
        TunProxyVpnService.stop(this);
    }

    public void startVpn(View view) {
        Intent i = VpnService.prepare(this);
        if (i != null) {
            startActivityForResult(i, REQUEST_VPN);
        } else {
            onActivityResult(REQUEST_VPN, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_VPN && parseAndSaveHostPort()) {
            TunProxyVpnService.start(this);
        }
    }

    private void loadHostPort() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String proxyHost = prefs.getString(TunProxyVpnService.PREF_PROXY_HOST, "");
        int proxyPort = prefs.getInt(TunProxyVpnService.PREF_PROXY_PORT, 0);

        if (TextUtils.isEmpty(proxyHost)) {
            return;
        }
        hostEditText.setText(getString(R.string.proxy, proxyHost, proxyPort));
        hostEditText.setError(null);
    }

    private boolean parseAndSaveHostPort() {
        String hostPort = hostEditText.getText().toString();
        if (!IPUtil.isValidIPv4Address(hostPort)) {
            hostEditText.setError(getString(R.string.enter_host));
            return false;
        }
        String[] parts = hostPort.split(":");
        int port = 0;
        if (parts.length > 1) {
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                hostEditText.setError(getString(R.string.enter_host));
                return false;
            }
        }
        String host = parts[0];
        sharedPrefService.saveHostPort(host, port);
        return true;
    }
}
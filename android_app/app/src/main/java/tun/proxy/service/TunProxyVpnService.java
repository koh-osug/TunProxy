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
package tun.proxy.service;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import tun.proxy.R;
import tun.proxy.di.DaggerWrapper;
import tun.proxy.event.SingleLiveEvent;
import tun.proxy.model.AppState;

/**
 * VPNService.
 *
 * @author <a href="mailto:raise.isayan@gmail.com">raise.isayan@gmail.com</a>
 */
public class TunProxyVpnService extends VpnService {
    public static final String PREF_PROXY_HOST = "pref_proxy_host";
    public static final String PREF_PROXY_PORT = "pref_proxy_port";
    public static final String PREF_LOG_LEVEL = "pref_log_level";
    public static final String PREF_RUNNING = "pref_running";

    private static final String TAG = TunProxyVpnService.class.getName();

    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    private long jniContext;
    private Thread tunnelThread;
    private ParcelFileDescriptor vpn;

    private ConnectivityManager cm;
    private ConnectivityManager.NetworkCallback networkCallback;
    private Network currentNetwork;

    @Inject
    AppState appState;

    @Inject
    SingleLiveEvent<Exception> exceptionSingleLiveEvent;

    @Inject
    DnsService dnsService;

    @Inject
    SharedPrefService sharedPrefService;

    static {
        System.loadLibrary("tun2http");
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroy");
        stop();
        jni_done(jniContext);
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        // Native init
        jniContext = jni_init(Build.VERSION.SDK_INT);
        super.onCreate();
        cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    if (currentNetwork != null && currentNetwork.equals(network)) {
                        Log.i(TAG, "Network connection lost.");
                        stop();
                        start();
                    }
                }

                @Override
                public void onAvailable(@NonNull Network network) {
                    Network _currentNetwork = cm.getActiveNetwork();
                    // new current network and we are not using it
                    if (network.equals(_currentNetwork) &&
                            (currentNetwork == null || !currentNetwork.equals(network))) {
                        Log.d(TAG, "Reconnecting after connection loss.");
                        stop();
                        start();
                    }
                }
            };
        }
        DaggerWrapper.getComponent(this).inject(this);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, TunProxyVpnService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, TunProxyVpnService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    private void start() {
        try {
            appState.setBusy(true);
            if (vpn == null) {
                Log.i(TAG, "Starting VPN");
                try {
                    vpn = getBuilder().establish();
                    startNative(vpn);
                } catch (Exception ex) {
                    Log.e(TAG, "Could not start VPN.", ex);
                    exceptionSingleLiveEvent.postValue(ex);
                    return;
                }
                appState.setProxyRunning(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    currentNetwork = cm.getActiveNetwork();
                    NetworkRequest.Builder builder = new NetworkRequest.Builder();
                    builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    cm.registerNetworkCallback(builder.build(), networkCallback);
                }
            }
        } finally {
            appState.setBusy(false);
        }
    }

    private void stop() {
        try {
            appState.setBusy(true);
            if (vpn != null) {
                try {
                    Log.i(TAG, "Stopping VPN");
                    stopNative(vpn);
                    try {
                        vpn.close();
                    } catch (IOException ex) {
                        Log.e(TAG, "Error stopping VPN.", ex);
                        exceptionSingleLiveEvent.postValue(ex);
                    }
                    if (networkCallback != null) {
                        cm.unregisterNetworkCallback(networkCallback);
                    }
                    vpn = null;
                } finally {
                    appState.setProxyRunning(false);
                }
            }
            stopForeground(true);
        } finally {
            appState.setBusy(false);
        }
    }

    @Override
    public void onRevoke() {
        Log.i(TAG, "Revoke");
        stop();
        super.onRevoke();
    }

    private Builder getBuilder() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Build VPN service
        Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));

        // VPN address
        String vpn4 = prefs.getString("vpn4", "10.1.10.1");
        builder.addAddress(vpn4, 32);
        String vpn6 = prefs.getString("vpn6", "fd00:1:fd00:1:fd00:1:fd00:1");
        builder.addAddress(vpn6, 128);

        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("0:0:0:0:0:0:0:0", 0);

        List<String> dnsList = dnsService.getDefaultDNS();
        for (String dns : dnsList) {
            Log.i(TAG, "default DNS:" + dns);
            builder.addDnsServer(dns);
        }

        // MTU
        int mtu = jni_get_mtu();
        Log.d(TAG, "MTU=" + mtu);
        builder.setMtu(mtu);

        // Add list of allowed and disallowed applications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (sharedPrefService.loadVPNMode() == SharedPrefService.VPNMode.DISALLOW) {
                Set<String> disallow = sharedPrefService.loadVPNApplication(SharedPrefService.VPNMode.DISALLOW);
                Log.d(TAG, "disallowed:" + disallow.size());
                List<String> notFoundPackageList = new ArrayList<>();
                builder.addDisallowedApplication(Arrays.asList(disallow.toArray(new String[0])), notFoundPackageList);
                disallow.removeAll(notFoundPackageList);
                sharedPrefService.storeVPNApplication(SharedPrefService.VPNMode.DISALLOW, disallow);
            } else {
                Set<String> allow = sharedPrefService.loadVPNApplication(SharedPrefService.VPNMode.ALLOW);
                Log.d(TAG, "allowed:" + allow.size());
                List<String> notFoundPackageList = new ArrayList<>();
                builder.addAllowedApplication(Arrays.asList(allow.toArray(new String[0])), notFoundPackageList);
                allow.removeAll(notFoundPackageList);
                sharedPrefService.storeVPNApplication(SharedPrefService.VPNMode.ALLOW, allow);
            }
        }

        // Add list of allowed applications
        return builder;
    }

    private void startNative(final ParcelFileDescriptor vpn) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String proxyHost = prefs.getString(PREF_PROXY_HOST, "");
        int proxyPort = prefs.getInt(PREF_PROXY_PORT, 0);
        int logLevel = Integer.parseInt(prefs.getString(PREF_LOG_LEVEL, Integer.toString(Log.WARN)));
        if (proxyPort != 0 && !TextUtils.isEmpty(proxyHost)) {
            jni_socks5(proxyHost, proxyPort, "", "");
            prefs.edit().putBoolean(PREF_RUNNING, true).apply();
            if (tunnelThread == null) {
                Log.i(TAG, "Starting tunnel thread context=" + jniContext);
                jni_start(jniContext, logLevel);

                tunnelThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Running tunnel context=" + jniContext);
                        try {
                            jni_run(jniContext, vpn.getFd(), false, 3);
                        } catch (Throwable e) {
                            Log.e(TAG, "Tunnel thread raised exception.", e);
                        }
                        Log.i(TAG, "Tunnel exited");
                        tunnelThread = null;
                    }
                });
                tunnelThread.start();
                Log.i(TAG, "Started tunnel thread");
            }

        }
    }

    private void stopNative(ParcelFileDescriptor vpn) {
        Log.i(TAG, "Stop native");

        if (tunnelThread != null) {
            Log.i(TAG, "Stopping tunnel thread");

            jni_stop(jniContext);

            Thread thread = tunnelThread;
            while (thread != null && thread.isAlive()) {
                try {
                    Log.i(TAG, "Joining tunnel thread context=" + jniContext);
                    thread.join();
                } catch (InterruptedException ignored) {
                    Log.i(TAG, "Joined tunnel interrupted");
                }
                thread = tunnelThread;
            }
            tunnelThread = null;

            jni_clear(jniContext);

            Log.i(TAG, "Stopped tunnel thread");
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(PREF_RUNNING, false).apply();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received " + intent);
        // Handle service restart
        if (intent == null) {
            return START_STICKY;
        }

        if (ACTION_START.equals(intent.getAction())) {
            start();
        }
        if (ACTION_STOP.equals(intent.getAction())) {
            stop();
        }
        return START_STICKY;
    }

    public class ServiceBinder extends Binder {
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            // see Implementation of android.net.VpnService.Callback.onTransact()
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        public TunProxyVpnService getService() {
            return TunProxyVpnService.this;
        }
    }

    private class Builder extends VpnService.Builder {

        private Builder() {
            super();
        }

        @Override
        public VpnService.Builder setMtu(int mtu) {
            super.setMtu(mtu);
            return this;
        }

        @Override
        public Builder addAddress(String address, int prefixLength) {
            super.addAddress(address, prefixLength);
            return this;
        }

        @Override
        public Builder addRoute(String address, int prefixLength) {
            super.addRoute(address, prefixLength);
            return this;
        }

        @Override
        public Builder addDnsServer(InetAddress address) {
            super.addDnsServer(address);
            return this;
        }

        @Override
        public Builder addDnsServer(String address) {
            super.addDnsServer(address);
            return this;
        }

        // min sdk 26
        public void addAllowedApplication(final List<String> packageList, final List<String> notFoundPackageList) {
            for (String pkg : packageList) {
                try {
                    Log.i(TAG, "allowed:" + pkg);
                    addAllowedApplication(pkg);
                } catch (PackageManager.NameNotFoundException e) {
                    notFoundPackageList.add(pkg);
                }
            }
        }

        public void addDisallowedApplication(final List<String> packageList, final List<String> notFoundPackageList) {
            for (String pkg : packageList) {
                try {
                    Log.i(TAG, "disallowed:" + pkg);
                    addDisallowedApplication(pkg);
                } catch (PackageManager.NameNotFoundException e) {
                    notFoundPackageList.add(pkg);
                }
            }
        }
    }

    // Called from native code
    @TargetApi(Build.VERSION_CODES.Q)
    private int getUidQ(int version, int protocol, String saddr, int sport, String daddr, int dport) {
        if (protocol != 6 /* TCP */ && protocol != 17 /* UDP */)
            return Process.INVALID_UID;

        InetSocketAddress local = new InetSocketAddress(saddr, sport);
        InetSocketAddress remote = new InetSocketAddress(daddr, dport);

        Log.d(TAG, "Get uid local=" + local + " remote=" + remote);
        int uid = cm.getConnectionOwnerUid(protocol, local, remote);
        Log.d(TAG, "Get uid=" + uid);
        return uid;
    }

    private native long jni_init(int sdk);

    private native void jni_start(long context, int logLevel);

    private native void jni_stop(long context);

    private native void jni_clear(long context);

    private native int jni_get_mtu();

    private native void jni_done(long context);

    private native void jni_run(long context, int tun, boolean fwd53, int rcode);

    private native void jni_socks5(String addr, int port, String username, String password);

}

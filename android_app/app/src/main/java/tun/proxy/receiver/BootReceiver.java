package tun.proxy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import androidx.preference.PreferenceManager;
import android.util.Log;
import tun.proxy.R;

import tun.proxy.service.TunProxyVpnService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent != null && !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isRunning = prefs.getBoolean(TunProxyVpnService.PREF_RUNNING, false);
        if (isRunning) {
            Intent prepare = VpnService.prepare(context);
            if (prepare == null) {
                Log.d(context.getString(R.string.app_name) + ".Boot", "Starting vpn");
                TunProxyVpnService.start(context);
            } else {
                Log.d(context.getString(R.string.app_name) + ".Boot", "Not prepared");
            }
        }
    }
}
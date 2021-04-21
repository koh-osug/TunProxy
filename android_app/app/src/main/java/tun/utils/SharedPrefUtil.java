package tun.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import tun.proxy.service.TunProxyVpnService;

/**
 * Utility for shared prefs.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class SharedPrefUtil {

    private SharedPrefUtil() {
    }

    public static boolean saveHostPort(String host, int port, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(TunProxyVpnService.PREF_PROXY_HOST, host);
        edit.putInt(TunProxyVpnService.PREF_PROXY_PORT, port);
        edit.apply();
        return true;
    }
}

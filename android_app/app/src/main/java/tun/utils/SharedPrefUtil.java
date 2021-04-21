package tun.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import tun.proxy.MyApplication;
import tun.proxy.service.TunProxyVpnService;

/**
 * Utility for shared prefs.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class SharedPrefUtil {

    public enum VPNMode {DISALLOW, ALLOW};

    private final static String PREF_VPN_MODE = "pref_vpn_connection_mode";
    private final static String[] PREF_APP_KEY = {"pref_vpn_disallowed_application", "pref_vpn_allowed_application"};

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

    public static VPNMode loadVPNMode(Context context) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String vpn_mode = sharedPreferences.getString(PREF_VPN_MODE, VPNMode.DISALLOW.name());
        return VPNMode.valueOf(vpn_mode);
    }

    public static void storeVPNMode(VPNMode mode, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_VPN_MODE, mode.name()).apply();
    }

    public static Set<String> loadVPNApplication(VPNMode mode, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet(PREF_APP_KEY[mode.ordinal()], new HashSet<String>());
    }

    public static void storeVPNApplication(VPNMode mode, final Set<String> set, Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(PREF_APP_KEY[mode.ordinal()], set).apply();
    }
}

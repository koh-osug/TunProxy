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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import tun.proxy.service.TunProxyVpnService;

/**
 * Utility for shared prefs.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
@Singleton
public class SharedPrefService {

    public enum VPNMode {DISALLOW, ALLOW};

    private final static String PREF_VPN_MODE = "pref_vpn_connection_mode";
    private final static String[] PREF_APP_KEY = {"pref_vpn_disallowed_application", "pref_vpn_allowed_application"};

    private Context context;

    @Inject
    public SharedPrefService(Context context) {
        this.context = context;
    }

    public boolean saveHostPort(String host, int port) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(TunProxyVpnService.PREF_PROXY_HOST, host);
        edit.putInt(TunProxyVpnService.PREF_PROXY_PORT, port);
        edit.apply();
        return true;
    }

    public VPNMode loadVPNMode() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String vpn_mode = sharedPreferences.getString(PREF_VPN_MODE, VPNMode.DISALLOW.name());
        return VPNMode.valueOf(vpn_mode);
    }

    public void storeVPNMode(VPNMode mode) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_VPN_MODE, mode.name()).apply();
    }

    public Set<String> loadVPNApplication(VPNMode mode) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet(PREF_APP_KEY[mode.ordinal()], new HashSet<String>());
    }

    public void storeVPNApplication(VPNMode mode, final Set<String> set) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(PREF_APP_KEY[mode.ordinal()], set).apply();
    }
}

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
package tun.proxy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import androidx.preference.PreferenceManager;
import android.util.Log;
import tun.proxy.R;

import tun.proxy.service.TunProxyRemoteService;
import tun.proxy.service.TunProxyVpnService;

/**
 *
 * Starts TunProxy if running on boot is enabled.
 *  @author <a href="mailto:raise.isayan@gmail.com">raise.isayan@gmail.com</a>
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BootReceiver.class.getName();

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
                Log.d(TAG, "Starting vpn");
                TunProxyVpnService.start(context);
            } else {
                Log.d(TAG, "Not prepared");
            }
        }
    }
}
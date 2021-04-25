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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.List;

import tun.proxy.VpnPermissionSupportActivity;
import tun.proxy.api.ITunProxyRemoteService;
import tun.utils.SharedPrefUtil;

/**
 * Service for starting the VPN remotely.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class TunProxyRemoteService extends Service {

    private static final String TAG = TunProxyRemoteService.class.getName();

    public static final String VPN_ALLOWED_BROADCAST = "TUN_PROXY_VPN_ALLOWED_BROADCAST_ACTION";

    private final BroadcastReceiver vpnAllowedOnActivityResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Starting remotely triggered service.");
            TunProxyVpnService.start(TunProxyRemoteService.this);
        }
    };

    private final ITunProxyRemoteService.Stub binder = new ITunProxyRemoteService.Stub() {

        private void requestVpnPermission() {
            Intent i = new Intent(TunProxyRemoteService.this, VpnPermissionSupportActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TunProxyRemoteService.this.startActivity(i);
        }

        @Override
        public void startAllowed(String ip, int port, List allowedApps) throws RemoteException {
            SharedPrefUtil.storeVPNMode(SharedPrefUtil.VPNMode.ALLOW, TunProxyRemoteService.this);
            SharedPrefUtil.saveHostPort(ip, port, TunProxyRemoteService.this);
            SharedPrefUtil.storeVPNApplication(SharedPrefUtil.VPNMode.ALLOW, new HashSet<String>(allowedApps), TunProxyRemoteService.this);
            requestVpnPermission();
        }

        @Override
        public void startDenied(String ip, int port, List deniedApps) throws RemoteException {
            SharedPrefUtil.storeVPNMode(SharedPrefUtil.VPNMode.DISALLOW, TunProxyRemoteService.this);
            SharedPrefUtil.storeVPNApplication(SharedPrefUtil.VPNMode.DISALLOW, new HashSet<String>(deniedApps), TunProxyRemoteService.this);
            SharedPrefUtil.saveHostPort(ip, port, TunProxyRemoteService.this);
            requestVpnPermission();
        }

        @Override
        public void stop() throws RemoteException {
            TunProxyVpnService.stop(TunProxyRemoteService.this);
        }
    };

    @Override
    public void onCreate() {
        IntentFilter i = new IntentFilter();
        i.addAction(VPN_ALLOWED_BROADCAST);
        registerReceiver(vpnAllowedOnActivityResult, i);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(vpnAllowedOnActivityResult);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

}

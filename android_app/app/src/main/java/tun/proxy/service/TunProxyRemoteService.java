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

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;

import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.inject.Inject;

import lombok.AllArgsConstructor;
import tun.proxy.VpnPermissionSupportActivity;
import tun.proxy.api.IStartStopCallback;
import tun.proxy.api.ITunProxyRemoteService;
import tun.proxy.di.DaggerWrapper;
import tun.proxy.model.AppState;
import tun.proxy.model.VpnGrantState;

/**
 * Service for starting the VPN remotely.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class TunProxyRemoteService extends LifecycleService {

    private static final String TAG = TunProxyRemoteService.class.getName();

    @Inject
    AppState appState;

    @Inject
    SharedPrefService sharedPrefService;

    @Inject
    VpnGrantState vpnGrantState;

    @AllArgsConstructor
    private class StartStopCallbackObserver implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            VpnGrantState vpnGrantState = (VpnGrantState)o;
            appState.setStartedRemotely(vpnGrantState.isVpnGranted());
            try {
                if (vpnGrantState.isVpnGranted()) {
                    vpnGrantState.getCb().onSuccess();
                    Log.i(TAG, "Starting remotely triggered service.");
                    TunProxyVpnService.start(TunProxyRemoteService.this);
                } else {
                    vpnGrantState.getCb().onPermissionDenied();
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not invoke callback.", e);
            }
        }
    }

    private final ITunProxyRemoteService.Stub binder = new ITunProxyRemoteService.Stub() {

        private void requestVpnPermission() {
            Intent i = new Intent(TunProxyRemoteService.this, VpnPermissionSupportActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            TunProxyRemoteService.this.startActivity(i);
        }

        @Override
        public void startAllowed(String ip, int port, List allowedApps, final IStartStopCallback cb) throws RemoteException {
            sharedPrefService.storeVPNMode(SharedPrefService.VPNMode.ALLOW);
            sharedPrefService.saveHostPort(ip, port);
            sharedPrefService.storeVPNApplication(SharedPrefService.VPNMode.ALLOW,
                    new HashSet<String>(allowedApps));
            vpnGrantState.setCb(cb);
            requestVpnPermission();
        }

        @Override
        public void startDenied(String ip, int port, List deniedApps, IStartStopCallback cb) throws RemoteException {
            sharedPrefService.storeVPNMode(SharedPrefService.VPNMode.DISALLOW);
            sharedPrefService.storeVPNApplication(SharedPrefService.VPNMode.DISALLOW,
                    new HashSet<String>(deniedApps));
            sharedPrefService.saveHostPort(ip, port);
            vpnGrantState.setCb(cb);
            requestVpnPermission();
        }

        @Override
        public void stop(IStartStopCallback cb) throws RemoteException {
            TunProxyVpnService.stop(TunProxyRemoteService.this);
            appState.setStartedRemotely(false);
            cb.onSuccess();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerWrapper.getComponent(this).inject(this);
        vpnGrantState.addObserver(new StartStopCallbackObserver());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        vpnGrantState.deleteObservers();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return binder;
    }

}

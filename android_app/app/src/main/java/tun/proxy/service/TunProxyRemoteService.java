package tun.proxy.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.util.List;

import tun.proxy.api.ITunProxyRemoteService;

/**
 * Service for starting the VPN remotely.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class TunProxyRemoteService extends Service {

    private static final String TAG = TunProxyRemoteService.class.getName();

    private final ITunProxyRemoteService.Stub binder = new ITunProxyRemoteService.Stub() {

        @Override
        public void startAllowed(String ip, int port, List allowedApps) throws RemoteException {
            TunProxyVpnService.start(TunProxyRemoteService.this);
        }

        @Override
        public void startDenied(String ip, int port, List deniedApps) throws RemoteException {
            TunProxyVpnService.start(TunProxyRemoteService.this);
        }

        @Override
        public void stop() throws RemoteException {
            TunProxyVpnService.stop(TunProxyRemoteService.this);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

}

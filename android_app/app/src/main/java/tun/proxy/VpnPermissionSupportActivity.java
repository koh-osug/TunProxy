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

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import javax.inject.Inject;

import tun.proxy.di.DaggerWrapper;
import tun.proxy.event.HideAppEvent;
import tun.proxy.event.SingleLiveEvent;
import tun.proxy.service.TunProxyRemoteService;

/**
 * Transparent activity to handle the permission to open a VPN for the remote service.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class VpnPermissionSupportActivity extends ComponentActivity {

    private static final String TAG = VpnPermissionSupportActivity.class.getName();

    @Inject
    SingleLiveEvent<HideAppEvent> hideAppEventSingleLiveEvent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerWrapper.getComponent(this).inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Starting VPN permission activity.");
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent i = new Intent();
                            i.setAction(TunProxyRemoteService.VPN_ALLOWED_BROADCAST);
                            sendBroadcast(i);
                        }
                        finish();
                        hideAppEventSingleLiveEvent.postValue(new HideAppEvent());
                    }
                });
        Intent i = VpnService.prepare(this);
        // already prepared iff null
        if (i != null) {
            activityResultLauncher.launch(i);
        }
        else {
            // notify caller that we are already prepared
            i = new Intent();
            i.setAction(TunProxyRemoteService.VPN_ALLOWED_BROADCAST);
            sendBroadcast(i);
            finish();
            hideAppEventSingleLiveEvent.postValue(new HideAppEvent());
        }
    }


}

package tun.proxy;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import tun.proxy.service.TunProxyRemoteService;

/**
 * Transparent activity to handle the permission to open a VPN for the remote service.
 *
 * @author <a href="mailto:k_o_@users.sourceforge.net">Karsten Ohme (k_o_@users.sourceforge.net)</a>
 */
public class VpnPermissionSupportActivity extends ComponentActivity {

    private static final String TAG = VpnPermissionSupportActivity.class.getName();

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
                    }
                });
        Intent i = VpnService.prepare(this);
        // already prepared iff null
        if (i != null) {
            activityResultLauncher.launch(i);
        }
    }


}

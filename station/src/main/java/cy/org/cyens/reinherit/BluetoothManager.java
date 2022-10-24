package cy.org.cyens.reinherit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContracts;

import cy.org.cyens.common.BluetoothService;

public class BluetoothManager {
    //region Constants *****************************************************************************
    private static final int BLUETOOTH_REQUEST_CODE = 102;
    private static final int ACCESS_FINE_LOCATION_CODE = 103;
    private static final int ACCESS_COARSE_LOCATION_CODE = 104;
    //endregion Constants **************************************************************************
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;
    /**
     * Member object for the services
     */
    private BluetoothService mService = null;

    private Activity mParentActivity;

    public String mBluetoothName;

    public BluetoothManager(Activity parentActivity) {
        mParentActivity = parentActivity;

        requestPermissions();

        // Setup Bluetooth
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(mParentActivity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            mParentActivity.finish();
        }
    }

    public boolean requestPermissions(){
        // Required permissions for bluetooth access
        // Android >=10 requires location permissions for bluetooth scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mParentActivity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_REQUEST_CODE);
            } else {
                mParentActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
            }
        } else {
            mParentActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_CODE);
        }
        return true;
    }

    public void setName(String newName){
        mBluetoothName = newName;
    }

    public String getName(){
        return mBluetoothName;
    }

    public void sendDeviceNameOverBluetooth(){
        write(mBluetoothName.getBytes());
    }

    public void onStart(ComponentActivity parentActivity, Handler handler){
        if (mBluetoothAdapter == null) {
            return;
        }
        // If BT is not on, request that it be enabled.
        // setup() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            parentActivity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                }
            }).launch(enableIntent);
            // Otherwise, setup the session
        } else if (mService == null) {
// Initialize the BluetoothService to perform bluetooth connections
            mService = new BluetoothService(parentActivity, handler);
        }
    }

    public void onResume(){
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth services
                mService.start();
            }
        }
    }

    public void onDestroy(){
        if (mService != null) {
            mService.stop();
        }
    }

    @SuppressLint("MissingPermission")
    public void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 360);
            mParentActivity.startActivity(discoverableIntent);
        }
    }

    //region IO
    public void write(byte[] data){
        mService.write(data);
    }
    //endregion IO
}

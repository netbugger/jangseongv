package kr.jne.hs.jangseongv.rooftop;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity implements NavigationHost {

    private final static String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mMainBleService = null;
    private String mDeviceAddress;
    private RooftopData mRooftopData;

    // BackPress Event Handling
    private long pressedTime = 0;
    private OnBackPressedListener mBackListener;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mMainBleService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mMainBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                //Toast.makeText(getContext(), R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.i(TAG, "onServiceConnected");
            mMainBleService.connect(mDeviceAddress);
            //Toast.makeText(getContext(), "Service Connected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

            mMainBleService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mRooftopData = new RooftopData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container, new DevSelectFragment())
                .commit();
    }

    public interface OnBackPressedListener {
        public void onBack();
    }
    public void setOnBackPressedListener(OnBackPressedListener listener) {
        mBackListener = listener;
    }
    @Override
    public void onBackPressed() {
        // 다른 Fragment 에서 리스너를 설정했을 때 처리됩니다.
        if(mBackListener != null) {
            mBackListener.onBack();
        } else {
            super.onBackPressed();
            finish();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    /**
     * Navigate to the given fragment.
     *
     * @param fragment       Fragment to navigate to.
     * @param addToBackstack Whether or not the current fragment should be added to the backstack.
     */
    @Override
    public void navigateTo(Fragment fragment, boolean addToBackstack) {
        FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, fragment);

        if (addToBackstack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }

    public void setDeviceAddress(String str)
    {
        mDeviceAddress = str;
    }
    public String getDeviceAddress() {
        return mDeviceAddress;
    }
    public BluetoothDevice getBleDevice()
    {
        return (mBluetoothAdapter.getRemoteDevice(mDeviceAddress));
    }


    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public void bleBindService()
    {
        bindService(new Intent(this, BluetoothLeService.class), mServiceConnection,  BIND_AUTO_CREATE);
        Log.i(TAG, "Bindig called");
    }

    public void bleUnbindService()
    {
        unbindService(mServiceConnection);
        mMainBleService = null;
        Log.i(TAG, "UnBindig called");
    }

    public BluetoothLeService getBleService()
    {
        return mMainBleService;
    }

    public RooftopData getRooftopData()
    {
        return mRooftopData;
    }

}

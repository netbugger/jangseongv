package kr.jne.hs.jangseongv.rooftop;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.UUID;

public class RooftopControlFragment extends Fragment implements MainActivity.OnBackPressedListener {
    /*
            Const Variables
     */
    private final static String TAG = "CONTROL";
    private final static int STATE_STOP = 0;
    private final static int CMD_STOP   = 0;
    private final static int STATE_UP = 1;
    private final static int CMD_UP   = 1;
    private final static int STATE_DOWN = 2;
    private final static int CMD_DOWN   = 2;


    /*
        Bluetooth Interface
     */
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattService mBleSvcCtrl;
    private BluetoothGattCharacteristic mBleCharCmd;

    /*
            Top Line Layout
     */
    private MaterialButton mBtnConnect;

    /*
            Mid Line Layout
     */
    private MaterialButton mBtnUp;
    private MaterialButton mBtnDown;

    /*
            State Variables
     */
    private boolean mConnection = false;
    private byte mState = STATE_STOP;

    private int mBtnState;

    private MainActivity mMain;



    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mMain = (MainActivity)getActivity();

        View view = inflater.inflate(R.layout.rooftop_control_fragment, container, false);
        mBtnConnect = view.findViewById(R.id.btn_connect);
        mBtnConnect.setEnabled(true);
        mBtnConnect.setBackgroundColor(Color.LTGRAY);

        mBtnUp = (MaterialButton)view.findViewById(R.id.control_btn_up);
        mBtnDown = (MaterialButton)view.findViewById(R.id.control_btn_down);

        mMain.bleBindService();
        mBluetoothLeService = mMain.getBleService();
        if(mBluetoothLeService != null && mBluetoothLeService.getConnectionState() == BluetoothLeService.BLE_STATE_CONNECTED) {
            mBleSvcCtrl = mBluetoothLeService.getService(RooftopData.UUID_ROOFTOP_CTRL_SVC);
            mBleCharCmd = mBleSvcCtrl.getCharacteristic(UUID.fromString(RooftopData.UUID_ROOFTOP_CMD));
            mConnection = true;
            updateConnectionStateUI();
        }

        mBtnUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendCmdMessage(CMD_UP);
            }
        });

        mBtnDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendCmdMessage(CMD_DOWN);
            }
        });


        mBtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if(!mConnection && mBluetoothLeService != null) {
                mBluetoothLeService.connect(mMain.getDeviceAddress());
            }
            }
        });
        return view;
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_NOTI_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DESCRIPTOR_WRITTEN);
        intentFilter.addAction(BluetoothLeService.ACTION_STATE_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

        return intentFilter;
    }


    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }
            else if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnection = true;
                updateConnectionStateUI();
                mBluetoothLeService = mMain.getBleService();
                Log.i(TAG, "Connected braoad received");

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnection = false;
                updateConnectionStateUI();
                Log.i(TAG, "DisConnected braoad received");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                mBleSvcCtrl = mBluetoothLeService.getService(RooftopData.UUID_ROOFTOP_CTRL_SVC);
                if(mBleSvcCtrl == null) {
                    Log.e(TAG, "getService Fail");
                }
                mBleCharCmd = mBleSvcCtrl.getCharacteristic(UUID.fromString(RooftopData.UUID_ROOFTOP_CMD));
            }
            else if( BluetoothLeService.ACTION_DESCRIPTOR_WRITTEN.equals(action)) {
                //startReadTimer(true, READ_TIMER_PERIOD);
            }
            else if (BluetoothLeService.ACTION_NOTI_DATA_AVAILABLE.equals(action)) {
                byte data[] = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
            }
            else {
                Log.e(TAG, "Noti data null");
            }
        }
    };



    public void SendCmdMessage(int command)
    {
        byte[] val = new byte[1];

        if(mConnection) {
            switch(command) {
                case CMD_UP :
                    val[0] = 'u';
                    break;
                case CMD_DOWN :
                    val[0] = 'd';
                    break;
            }
            mBleCharCmd.setValue(val);
            mBluetoothLeService.writeCharacteristic(mBleCharCmd);
        }
        else {
            Toast.makeText(getContext(), "Bluetooth Not Connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateConnectionStateUI()
    {
        if(mConnection) {
            mBtnConnect.setEnabled(false);
            mBtnConnect.setIconResource(R.drawable.bt_conn);
            mBtnConnect.setText(getString(R.string.ble_state_connected));
            mBtnConnect.setBackgroundColor(Color.CYAN);
            mBtnDown.setEnabled(true);
            mBtnUp.setEnabled(true);
        }
        else {
            mBtnConnect.setEnabled(true);
            mBtnConnect.setIconResource(R.drawable.bt_disconn);
            mBtnConnect.setText(getString(R.string.ble_state_disconnected));
            mBtnConnect.setBackgroundColor(Color.LTGRAY);
            mBtnDown.setEnabled(false);
            mBtnUp.setEnabled(false);
        }
    }


    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        Log.i(TAG, "onAttach()");
        ((MainActivity)context).setOnBackPressedListener(this);
    }

    @Override
    public void onBack()
    {
        Log.i(TAG, "onBack()");
        //서비스 언바인드
        mMain.bleUnbindService();
        mMain.finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onResume()
    {
        Log.i(TAG, "OnResume()");
        getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mBluetoothLeService = mMain.getBleService();
        if(mBluetoothLeService != null) {
            mBleSvcCtrl = mBluetoothLeService.getService(RooftopData.UUID_ROOFTOP_CTRL_SVC);
            mBleCharCmd = mBleSvcCtrl.getCharacteristic(UUID.fromString(RooftopData.UUID_ROOFTOP_CMD));
            if(mBluetoothLeService.getConnectionState() == BluetoothLeService.BLE_STATE_CONNECTED) {
                mConnection = true;
            }
            else {
                mConnection = false;
            }

            Handler delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateConnectionStateUI();
                }
            }, 500);
        }
        super.onResume();
    }

    @Override
    public void onPause()
    {
        Log.i(TAG, "OnPause()");
        super.onPause();
    }

    /*
        onDestroy called when navigate to other Fragment
     */
    @Override
    public void onDestroy()
    {
        Log.i(TAG, "OnDestroy()");
        if(mGattUpdateReceiver != null) {
            getActivity().unregisterReceiver(mGattUpdateReceiver);
        }
        mBluetoothLeService = null;
        super.onDestroy();
    }
}
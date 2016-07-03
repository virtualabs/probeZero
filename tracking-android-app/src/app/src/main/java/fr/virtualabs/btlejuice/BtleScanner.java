package fr.virtualabs.btlejuice;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class BtleScanner extends AppCompatActivity {

    private DeviceAdapter mAdapter;
    private ListView mList;
    private TextView mStatusBar;
    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private BluetoothLeScanner mLEScanner;
    private android.os.Handler mHandler;
    private boolean mScanning;
    private ScanSettings mSettings;
    private List<ScanFilter> mFilters;
    private DeviceRanging mRanging;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btle_scanner);

        mAdapter = new DeviceAdapter(
                this,
                R.layout.device_info,
                new ArrayList<DeviceInfo>()
        );

        mHandler = new Handler();
        mList=(ListView)findViewById(R.id.deviceList);
        mList.setAdapter(mAdapter);
        mStatusBar = (TextView)findViewById(R.id.status);

        mRanging = new DeviceRanging(mAdapter, mList, mStatusBar, getApplicationContext());
        mRanging.scanLeDevice(true);

    }


    @Override
    protected void onResume() {
        super.onResume();
        mRanging.resumeScan();
    }

    @Override
    protected void onDestroy() {
        mRanging.scanLeDevice(false);
        super.onDestroy();
    }

    public void setStatus(String status) {
        mStatusBar.setText(status);
    }

}

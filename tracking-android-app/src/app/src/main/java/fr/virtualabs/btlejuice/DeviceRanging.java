package fr.virtualabs.btlejuice;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

import fr.virtualabs.btlejuice.fingerprints.Final;
import fr.virtualabs.btlejuice.fingerprints.NameModelManufacturer;
import fr.virtualabs.btlejuice.fingerprints.PassiveFingerprinter;
import fr.virtualabs.btlejuice.utils.ADParser;

/**
 * Created by virtualabs on 29/01/16.
 *
 * This class maintains a list of potential devices, connect to them and performs some kind
 * of fingerprinting. It provides the UI through a list adapter in order to refresh the view.
 */

@TargetApi(21)
public class DeviceRanging {

    private HashMap<String, DeviceInfo> mDetectedDevices;
    private ArrayAdapter mUiAdapter;
    private ListView mUiList;
    private TextView mUiStatus;
    private Context mContext;
    private Stack<String> mToProcess;
    private Thread mFingerprint;

    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private BluetoothLeScanner mLEScanner;
    private BluetoothGatt mGatt;
    private android.os.Handler mHandler;
    private ScanSettings mSettings;
    private List<ScanFilter> mFilters;

    private static class DeviceModel {
        private DeviceInfo.DEVICE_TYPE mType;
        private String mName;

        DeviceModel(DeviceInfo.DEVICE_TYPE type, String name) {
            mType = type;
            mName = name;
        }
        public String getName() { return mName; }
        public DeviceInfo.DEVICE_TYPE getType() { return mType; }
    }

    private HashMap<String, DeviceModel> mSignatures;

    private enum STATUS {
        IDLE,
        SCANNING,
        FINGER,
    };

    private STATUS mStatus;

    private final UUID GAS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private final UUID DEVICE_NAME = UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb");
    private final UUID MANUFACTURER = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private final UUID MODEL_NUMBER = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");

    public DeviceRanging(ArrayAdapter adapter, ListView list, TextView status, Context context) {
        mUiAdapter = adapter;
        mUiList = list;
        mUiStatus = status;
        mContext = context;
        mStatus = STATUS.IDLE;
        mDetectedDevices = new HashMap<String, DeviceInfo>();
        mSignatures = new HashMap<String, DeviceModel>();
        mToProcess = new Stack<String>();

        populateSignatures();

        mFingerprint = new Thread(new Runnable(){
            public void run() {
                /* Loop and process devices. */
                while (true) {
                    DeviceRanging.this.fingerprintPendingDevice();
                }
            }
        });
        mFingerprint.start();

        /* Starts BTLE, get adapter. */
        mBleManager =(BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        if (mBleAdapter == null || !mBleAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableBtIntent);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBleAdapter.getBluetoothLeScanner();
                mSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                mFilters = new ArrayList<ScanFilter>();
            }

            /* Request scanner if not already available. */
            if (Build.VERSION.SDK_INT < 21) {
                mLEScanner = null;
                mSettings = null;
                mFilters = null;
            } else {
                mLEScanner = mBleAdapter.getBluetoothLeScanner();
                mSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                mFilters = new ArrayList<ScanFilter>();
            }

            scanLeDevice(true);
        }

    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            setStatus("Scanning ...");
            if (Build.VERSION.SDK_INT < 21) {
                mBleAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(mFilters, mSettings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBleAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
        if (enable)
            mStatus = STATUS.SCANNING;
        else
            mStatus = STATUS.IDLE;
    }

    public void resumeScan() {
        if (mBleAdapter == null || !mBleAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableBtIntent);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBleAdapter.getBluetoothLeScanner();
                mSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                mFilters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice btDevice = result.getDevice();
            addOrUpdateDevice(
                    btDevice,
                    result.getScanRecord().getBytes(),
                    result.getTimestampNanos(),
                    result.getRssi()
            );
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                addOrUpdateDevice(
                        sr.getDevice(),
                        sr.getScanRecord().getBytes(),
                        sr.getTimestampNanos(),
                        sr.getRssi()
                );
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    addOrUpdateDevice(
                            device,
                            scanRecord,
                            System.nanoTime(),  /* Use current time. */
                            rssi
                    );

                }
            };


    /**
     * Add device to the detected devices list.
     *
     * @param device
     * @return
     */
    private void addDevice(BluetoothDevice device, byte[] scanRecord, long lLastSeen, int iRSSI) {
        /* Create device info from device */
        DeviceInfo devinfo = new DeviceInfo(
                device,
                null,
                device.getName(),
                null,
                lLastSeen,
                scanRecord,
                iRSSI,
                DeviceInfo.DEVICE_TYPE.UNKNOWN
        );

        /* Add device with associated device info. */
        mDetectedDevices.put(device.getAddress(), devinfo);
    }

    /**
     * Add or update device.
     *
     * @param device
     */
    public void addOrUpdateDevice(BluetoothDevice device, byte[] scanRecord, long lLastSeen, int iRSSI) {
        DeviceInfo dev;
        if (mDetectedDevices.containsKey(device.getAddress())) {
            dev = mDetectedDevices.get(device.getAddress());
            if (dev.isInUiList()) {
                /* Update device info if already in UI list. */
                Log.i("DeviceRanging", "New RSSI is " + String.valueOf(iRSSI));
                mDetectedDevices.get(device.getAddress()).setLastSeen(lLastSeen);
                mDetectedDevices.get(device.getAddress()).updateRSSI(iRSSI);
            }
        } else {
            /* Add device info. */
            Log.d("DeviceRanging", "Add device " + device.getAddress());
            addDevice(device, scanRecord, lLastSeen, iRSSI);
            /* Requires fingerprinting. */
            Log.d("DeviceRanging", "Requires fingerprinting for " + device.getAddress());
            mToProcess.push(device.getAddress());
        }
        updateAdapter();
    }

    public synchronized void fingerprintPendingDevice() {
        /* Pick a device to fingerprint. */
        if (!mToProcess.empty()) {
            Log.d("DeviceRanging", "Required fingerprinting");
            String device = mToProcess.pop();
            final DeviceInfo devinfo = mDetectedDevices.get(device);
            Log.d("DeviceRanging", "Fingerprinting device " + device);
            setStatus("Fingerprinting ...");
            scanLeDevice(false);

            /* Try a quick fingerprinting */
            for (Map.Entry<String, DeviceModel> entry : mSignatures.entrySet()) {
                String signature = entry.getKey();
                DeviceModel model = entry.getValue();

                if (compareSignature(ADParser.parse(devinfo.getScanRecord()), signature)) {
                    devinfo.setDevice(model.getName());
                    devinfo.setType(model.getType());

                    /* fingerprinted, stop right here. */
                    scanLeDevice(true);
                    return;
                }
            }

            /* Connect to device, query services and perform fingerprint. */
            if (devinfo.getRSSI() >= -90) {
                Log.i("Fingerprint", "Fingerprint device " + device);
                mStatus = STATUS.FINGER;

                PassiveFingerprinter pof = new PassiveFingerprinter(
                        devinfo,
                        new NameModelManufacturer(
                                mContext,
                                devinfo,
                                new Final(null)
                        )
                );
                pof.setCallback(new Fingerprint.FingerprintCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i("DeviceRanging", "Found device name !");
                        scanLeDevice(true);
                    }

                    @Override
                    public void onError() {
                        /*
                        Log.e("DeviceRanging", "Device name not found");
                        if (devinfo.getTries() < 2)
                            mToProcess.insertElementAt(devinfo.getAddress(), mToProcess.size());
                           */
                        scanLeDevice(true);
                    }
                });
                pof.perform();
            } else {
                Log.i("DeviceRanging", "RSSI too low for device " + device);
                mDetectedDevices.remove(device);
                scanLeDevice(true);
            }
        }
    }

    private synchronized void updateAdapter() {
        DeviceInfo devinfo;
        boolean needRefresh = false;
        String device;
        Iterator<String> deviceIter;
        ArrayList<String> snapshot = new ArrayList<String>();
        final ArrayList<DeviceInfo> itemsToAdd = new ArrayList<DeviceInfo>();

        /* Takes a snapshot of actually displayed devices. */
        for (int i=0; i<mUiAdapter.getCount(); i++) {
            devinfo = (DeviceInfo)mUiAdapter.getItem(i);

            if (!devinfo.isInUiList())
                devinfo.foundInUiList();

            snapshot.add(devinfo.getAddress());
        }

        /* Add if necessary. */
        deviceIter = mDetectedDevices.keySet().iterator();
        while (deviceIter.hasNext()) {
            device = deviceIter.next();
            if (!snapshot.contains(device) && !mDetectedDevices.get(device).isPendingList()) {
                Log.i("DeviceRanging","Found new device "+device);
                devinfo = mDetectedDevices.get(device);
                if ((devinfo.getDevice() != null) && (devinfo.getType() != DeviceInfo.DEVICE_TYPE.UNKNOWN)) {
                    itemsToAdd.add(devinfo);
                    devinfo.addedInList();
                }
            }
        }

        Log.d("DeviceRanging", "Notify dataset changed");
        mUiList.post(new Runnable() {
            public void run() {
                if (itemsToAdd.size() > 0) {
                    mUiAdapter.addAll(itemsToAdd);
                } else {
                    mUiAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void setStatus(String status) {
        final String status_ = status;
        mUiStatus.post(new Runnable() {
            public void run() {
                mUiStatus.setText(status_);
            }
        });
    }

    private void populateSignatures() {
        /* Fitbit */
        mSignatures.put(
                "06:ba5689a6fabfa2bd01467d6e00fbabad,09:436861726765204852,0a:fa,16:0a1812",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.FITBIT, "Fitbit Charge HR")
        );
        mSignatures.put(
                "06:ba5689a6fabfa2bd01467d6e00fbabad,09:436861726765,0A:fa,16:0a1808",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.FITBIT, "Fitbit Charge")
        );
        mSignatures.put(
                "09:4f6e65,0A:fa,16:0a180504",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.FITBIT, "Fitbit One")
        );
        mSignatures.put(
                "01:06,06:ba5689a6fabfa2bd01467d6ea753abad,09:466c6578,0A:fa,16:0a180704",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.FITBIT, "Fitbit Flex")
        );
        mSignatures.put(
                "01:06,06:ba5689a6fabfa2bd01467d6e00fbabad,09:5375726765,16:0a1810",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.FITBIT, "Fitbit Surge")
        );

        /* Trackers */
        mSignatures.put(
                "01:06,07:45fa56c1fb1bc02896a867180228174f,09:4741424c5953204c495445,19:0000",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.KEYFOB, "Gablys Lite")
        );
        mSignatures.put(
                "01:04,09:57697374696b692d,0A:00",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.KEYFOB, "Wistiki")
        );
        mSignatures.put(
                "01:06,02:0f18,09:4769676173657420472d746167,FF:800102151234",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.KEYFOB, "G-tag")
        );
        mSignatures.put(
                "01:06,06:d54e8938944fd483774f33f8d6851045,09:436869706f6c6f",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.KEYFOB, "Chipolo")
        );

        /* Smartwatch */
        mSignatures.put(
                "07:669a0c2000089a94e3117b66103e4e6a,09:66656e6978203300,16:103e001200",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.SMARTWATCH, "Garmin Fenix 3")
        );
        mSignatures.put(
                "07:669a0c2000089a94e3117b66103e4e6a,09:466f726572756e6e6572,16:103e000200",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.SMARTWATCH, "Garmin Forerunner 920")
        );
        mSignatures.put(
                "01:1a,FF:4c000c0e005c",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.SMARTWATCH, "Apple Watch")
        );
        mSignatures.put(
                "01:06,09:57204163746976697465,0A:00,FF:0024e436",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.SMARTWATCH, "Withings Activite")
        );

        /* Wristbands */
        mSignatures.put(
                "01:06,07:669a0c200008c181e211dd3110c4cd83",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.WRISTBAND, "Nike+ FuelBand SE")
        );
        mSignatures.put(
                "01:06,06:bc4f45f35650a19c1141804500101c15,09:555032,16:0a1802",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.WRISTBAND, "Jawbone UP2")
        );
        mSignatures.put(
                "01:06,06:bc4f45f35650a19c1141804500101c15,09:555033,16:0a1803",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.WRISTBAND, "Jawbone UP3")
        );
        mSignatures.put(
                "01:06,07:669a0c2000089a94e3117b66103e4e6a,16:103e001200",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.WRISTBAND, "vivosmart/vivoactive")
        );
        mSignatures.put(
                "01:06,02:f0ff,09:4a5354594c45",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.WRISTBAND, "JStyle Wristband")
        );

        /* Audio */
        mSignatures.put(
                "01:1a,03:0d180f180a18,03:0d180f180a18,09:4a61627261",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.SOUND, "Jabra Pulse")
        );
        mSignatures.put(
                "01:12,03:befe,09:426f73652041453220536f756e644c696e6b,FF:0033400a",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.SOUND, "Bose AE2 Soundlink")
        );

        /* MyFox */
        mSignatures.put(
                "01:05,07:1bc5d5a502003c98e41188880100a1e4,09:4d79666f78",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.KEYFOB, "Myfox Alarm remote")
        );

        /* iPhone */
        mSignatures.put(
                "01:1a,FF:4c000c0e",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.PHONE, "iPhone")
        );

        /* Wiko */
        mSignatures.put(
                "01:02,09:43494e4b20504541582032,09:43494e4b20504541582032",
                new DeviceModel(DeviceInfo.DEVICE_TYPE.PHONE, "Wiko Cink Peax 2")
        );
    }

    private boolean compareSignature(ArrayList<ADParser.Record> records, String signature) {
        boolean type_found = false;

        /* Convert signature to ADParser.Record list. */
        Log.d("SIGNATURE", signature);
        ArrayList<ADParser.Record> sig_records = new ArrayList<ADParser.Record>();
        String[] sig_records_tokens = signature.split(",");
        for (String record : sig_records_tokens) {
            /* Extract data type (first byte) then data (next bytes) */
            Log.d("TESTSIG", record.substring(0,2));
            int data_type = Integer.parseInt(record.substring(0,2), 16);
            /* Extract data */
            byte[] data = hexStringToByteArray(record.substring(3));
            sig_records.add(new ADParser.Record((byte)data_type, data));
        }

        /* For each AD record of the signature, find a matching one and check if data is OK. */
        for (ADParser.Record record : sig_records) {
            type_found = false;
            for (ADParser.Record my_record: records) {
                if (record.getType() == my_record.getType()) {
                    type_found = true;
                    if (!Arrays.equals(record.getData(),Arrays.copyOfRange(my_record.getData(), 0, record.getData().length)
                    ))  {
                        Log.d("TEST SIG", String.format("Data type %d are not the same", record.getType()));
                        Log.d("TEST SIG", "Signature: "+convertStringToHex(record.getData()));
                        Log.d("TEST SIG", "Scan data: "+convertStringToHex(my_record.getData()));
                        return false;
                    }
                }
            }
            if (type_found == false) {
                Log.d("TEST SIG", String.format("Data type of %d not found !", record.getType()));
                return false;
            }
        }
        return true;
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    private String convertStringToHex(byte[] buf)
    {
        final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i)
        {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }
}

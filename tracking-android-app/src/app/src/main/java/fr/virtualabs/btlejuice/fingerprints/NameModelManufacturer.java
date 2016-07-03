package fr.virtualabs.btlejuice.fingerprints;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import fr.virtualabs.btlejuice.DeviceInfo;
import fr.virtualabs.btlejuice.Fingerprint;
import fr.virtualabs.btlejuice.utils.FileLog;


public class NameModelManufacturer extends Fingerprint {

    private BluetoothGatt mGatt;
    private BluetoothDevice mDevice;
    private BluetoothGattCallback mGattCallback;
    private DeviceInfo mDevInfo;
    private Context mContext;
    private String sign;

    private ArrayList<String> mServicesList;

    private final UUID GAS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private final UUID DEVICE_NAME = UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb");
    private final UUID MANUFACTURER = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    private final UUID MODEL_NUMBER = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");

    private static class SignatureHandler {

        private String mSignature;

        public SignatureHandler(String signature) {
            mSignature = signature;
        }

        public void apply(){

        }

        public String getSignature() {
            return mSignature;
        }
    };

    private ArrayList<SignatureHandler> mSignatures;
    private ArrayList<SignatureHandler> mManufacturerSignatures;

    public NameModelManufacturer(Context context, DeviceInfo devinfo, Fingerprint fpNext) {
        super(fpNext);

        mDevice = devinfo.getObject();
        mDevInfo = devinfo;
        mContext = context;
        mSignatures = new ArrayList<SignatureHandler>();
        mManufacturerSignatures = new ArrayList<SignatureHandler>();
        mServicesList = new ArrayList<String>();

        sign = String.format("%s;%s;", mDevInfo.getAddress(), asHex(mDevInfo.getScanRecord()));

        //populateSignatures();

        /* Setup gatt callback. */
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Log.i("NameModelManufacturer","Connected to device");
                        gatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.i("NameModelManufacturer","Disconnected !");
                        Log.i("BtleJuice",sign);
                        FileLog.write(sign);

                        /* Unable to contact btle device, restart scanning. */
                        if (mDevInfo.getType() == DeviceInfo.DEVICE_TYPE.UNKNOWN) {
                            if (mDevInfo.getTries()<2)
                                mDevInfo.incTries();
                            NameModelManufacturer.this.processNext();
                        }
                        break;
                    default:
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                List<BluetoothGattService> services = gatt.getServices();

                /* Check if a custom service UUID matches a device type. */
                //ArrayList<String> servicesList = new ArrayList<String>();
                mServicesList.clear();
                for (BluetoothGattService service : services) {
                    Log.i("NameModelManufacturer","Service UUID:" + service.getUuid().toString().toUpperCase());
                    mServicesList.add(service.getUuid().toString().toUpperCase());
                }

                /* Log services */
                String msg ="";
                for (String service: mServicesList) {
                    msg += String.format("%s,", service);
                }
                if (msg.length() > 0)
                    sign += msg.substring(0, msg.length()-2)+";";
                else
                    sign += ";";

                /*
                for (SignatureHandler signature: mSignatures) {
                    if (mServicesList.contains(signature.getSignature())) {
                        signature.apply();
                        gatt.disconnect();
                        if (getCallback() != null)
                            getCallback().onSuccess();
                        return;
                    }
                }*/

                /* TODO: use GATT/MODEL_NUMBER or GATT/DEVICE_NAME to identify device. */
                if (gatt.getService(GAS) != null) {
                    BluetoothGattService gas = gatt.getService(GAS);
                    if (gas.getCharacteristic(MODEL_NUMBER) != null)
                        gatt.readCharacteristic(gas.getCharacteristic(MODEL_NUMBER));
                    else if (gas.getCharacteristic(DEVICE_NAME) != null)
                        gatt.readCharacteristic(gas.getCharacteristic(DEVICE_NAME));
                    else {
                        Log.i("SIGNATURE", "No device_name UUID");
                        gatt.disconnect();
                    }
                } else {
                    Log.i("SIGNATURE", "No GAS UUID");
                    gatt.disconnect();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic
                                                     characteristic, int status) {
                String modelNumber = new String(characteristic.getValue());
                Log.i("SIGNATURE-TESTING", "Device model number: " + modelNumber);
                /*
                for (SignatureHandler signature: mSignatures) {
                    if (signature.getSignature().equals(modelNumber)) {
                        signature.apply();
                        gatt.disconnect();
                        if (getCallback() != null)
                            getCallback().onSuccess();
                        return;
                    }
                }*/

                /* Log services */
                if (characteristic.getUuid().equals(DEVICE_NAME) && (modelNumber != null)) {
                    //String msg = String.format("Device Name: %s", modelNumber);
                    //FileLog.write(msg);
                    sign += "DN=" + modelNumber+";";
                } else if (characteristic.getUuid().equals(MODEL_NUMBER) && (modelNumber != null)) {
                    //String msg = String.format("Model Number: %s", modelNumber);
                    //FileLog.write(msg);
                    sign += "M#=" + modelNumber+";";
                }

                /* If not found and device_name characteristic, fingerprint as unknow and display its value. */
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.APPLE);
                mDevInfo.setDevice(modelNumber);
                if (getCallback() != null)
                    getCallback().onSuccess();

                gatt.disconnect();

            }
        };
    }

    public void perform() {
        mGatt = mDevice.connectGatt(mContext, false, mGattCallback);
    }

    public void populateSignatures() {

        /* iPhone */
        mSignatures.add(new SignatureHandler("89D3502B-0F36-433A-8EF4-C502AD55F8DC") {
            @Override
            public void apply() {
                Log.i("SIGNATURE-MATCHING", "Found iPhone");
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.APPLE);
                mDevInfo.setDevice("iPhone");
            }
        });

        /* Apple TV */
        mSignatures.add(new SignatureHandler("Apple TV") {
            @Override
            public void apply() {
                Log.i("SIGNATURE-MATCHING", "Found Apple TV");
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.APPLE);
                mDevInfo.setDevice("Apple TV");
            }
        });

        /* Apple Watch */
        mSignatures.add(new SignatureHandler("Watch1,2") {
            @Override
            public void apply() {
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.APPLE);
                mDevInfo.setDevice("Apple Watch");
            }
        });


        /* Fitbit */
        mSignatures.add(new SignatureHandler("ADAB5769-6E7D-4601-BDA2-BFFAA68956BA") {
            @Override
            public void apply() {
                Log.i("SIGNATURE-MATCHING", "Found Fitbit");
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.FITBIT);
                mDevInfo.setDevice("FitBit Flex/One");
            }
        });

        /* MacBookAir */
        mSignatures.add(new SignatureHandler("MacBookAir5,2") {
            @Override
            public void apply() {
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.APPLE);
                mDevInfo.setDevice("MacBook Air");
            }
        });


        /* Okidokeys */
        /*
        mSignatures.add(new SignatureHandler("0000FFF0-0000-1000-8000-00805F9B34FB") {
            @Override
            public void apply() {
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.SMARTLOCK);
                mDevInfo.setDevice("Okidokeys smartlock");
            }
        });
        */

        /* Polar H7 */
        mSignatures.add(new SignatureHandler("6217FF4B-FB31-1140-AD5A-A45545D7ECF3") {
            @Override
            public void apply() {
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.HEARTBEAT);
                mDevInfo.setDevice("Polar H6/H7");
            }
        });

        /* Gablys */
        mSignatures.add(new SignatureHandler("89943300-2D54-B8CB-3AF2-212144C5CA13") {
            @Override
            public void apply() {
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.KEYFOB);
                mDevInfo.setDevice("Gablys tracker");
            }
        });

                /* Gablys */
        mSignatures.add(new SignatureHandler("757ED3E4-1828-4A0C-8362-C229C3A6DA72") {
            @Override
            public void apply() {
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.SOUND);
                mDevInfo.setDevice("UE BOOM 2");
            }
        });

    }

    private String asHex(byte[] buf)
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

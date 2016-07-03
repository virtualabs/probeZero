package fr.virtualabs.btlejuice.fingerprints;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

import fr.virtualabs.btlejuice.DeviceInfo;
import fr.virtualabs.btlejuice.Fingerprint;

/**
 * Created by virtualabs on 02/02/16.
 */
public class Okidokeys extends Fingerprint {

    private BluetoothGatt mGatt;
    private DeviceInfo mDevInfo;

    public Okidokeys(BluetoothGatt gatt, DeviceInfo devinfo, Fingerprint fpNext) {
        super(fpNext);

        mGatt = gatt;
        mDevInfo = devinfo;
    }


    public void perform() {
        FingerprintCallback callback = getCallback();
        String deviceName= "";
        int i;
        byte[] scanRecord = mDevInfo.getScanRecord();
        Log.v("Okidokeys", String.format("%x %x", scanRecord[5], scanRecord[6]));
        if ((((int) scanRecord[5] & 0xff) == 0xf0) && (((int) scanRecord[6] & 0xff) == 0xff)) {
            deviceName = new String(Arrays.copyOfRange(scanRecord, 9, 21));
            Log.d("Okidokeys", "Device name is " + deviceName);
                    /* Check with device name. */
            if (mDevInfo.getObject().getName().equals(deviceName)) {
                mDevInfo.setType(DeviceInfo.DEVICE_TYPE.SMARTLOCK);
                mDevInfo.setDevice("Smartlock Okidokeys (" + deviceName + ")");
                if (callback != null)
                    callback.onSuccess();
            } else {
                processNext();
            }
        } else {
            processNext();
        }
    }

}

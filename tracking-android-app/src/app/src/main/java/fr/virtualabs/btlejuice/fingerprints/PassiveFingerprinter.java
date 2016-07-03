package fr.virtualabs.btlejuice.fingerprints;

import android.bluetooth.BluetoothClass;
import android.bluetooth.le.ScanRecord;
import android.util.Log;

import java.util.ArrayList;

import fr.virtualabs.btlejuice.DeviceInfo;
import fr.virtualabs.btlejuice.Fingerprint;
import fr.virtualabs.btlejuice.utils.ADParser;
import fr.virtualabs.btlejuice.utils.FileLog;

public class PassiveFingerprinter extends Fingerprint {
    private static class SignatureHandler {

        private String mSignature;

        /**
         * match
         *
         * Check if scanRecord match
         *
         * @param devinfo
         * @return
         */
        public boolean match(DeviceInfo devinfo) {
            return false;
        }

        public SignatureHandler(String signature) {
            mSignature = signature;
        }

        public String getSignature() {
            return mSignature;
        }
    };

    private ArrayList<SignatureHandler> mSignatures;
    private DeviceInfo mDevice;

    public PassiveFingerprinter(DeviceInfo devInfo, Fingerprint fpNext) {
        super(fpNext);

        mSignatures = new ArrayList<SignatureHandler>();
        mDevice = devInfo;

        /* Register signatures. */
        registerSignatures();
    }

    public void perform() {
        for (SignatureHandler sign: mSignatures) {
            if (sign.match(mDevice)) {
                if (getCallback() != null)
                    getCallback().onSuccess();
            } else {
                processNext();
            }
        }
    }

    public void registerSignatures() {

        mSignatures.add(new SignatureHandler("Apple Device"){
            @Override
            public boolean match(DeviceInfo devinfo) {
                /* Parse scanRecord. */
                ArrayList<ADParser.Record> records = ADParser.parse(devinfo.getScanRecord());
                /*
                for (ADParser.Record record: records) {
                    Log.i("POF-DETECT", String.format("Record type %02X", record.getType()));
                    if (record.getType() == (byte)0xFF) {
                        Log.i("POF-DETECT", String.format("%02X %02X", record.getData()[0], record.getData()[1]) );
                        if ((record.getData()[0] == 0x4C) && (record.getData()[1] == 0x00)) {
                            Log.i("PASSIVE_FP", "Record type is " + String.format("%02X", record.getType()));
                            devinfo.setDevice("Apple Device");
                            devinfo.setType(DeviceInfo.DEVICE_TYPE.APPLE);
                            return true;
                        }
                    }
                }*/
                String msg = String.format("%s;%s;;;", devinfo.getAddress(), convertStringToHex(devinfo.getScanRecord()));
                FileLog.write(msg);
                return false;
            }
        });
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

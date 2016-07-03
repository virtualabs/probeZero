package fr.virtualabs.btlejuice;

import android.bluetooth.BluetoothDevice;

/**
 * Device information class
 *
 * This class holds every piece of information required by the application.
 * The corresponding class instances are managed either by the main ListAdapter
 * and the ranging thread.
 */

public class DeviceInfo {
    private final String mAddress;
    private String mOwner;
    private String mDevice;
    private final BluetoothDevice mDeviceObj;
    private String mESN;
    private final byte[] mSR;

    private STATUS mStatus;
    private int mRSSI;
    private DEVICE_TYPE mType;
    private long mLastSeen;
    private int tries = 0;

    /**
     * Device types
     */
    public enum DEVICE_TYPE {
        UNKNOWN,
        PHONE,
        APPLE,
        FITBIT,
        SMARTLOCK,
        KEYFOB,
        WRISTBAND,
        HEARTBEAT,
        SMARTWATCH,
        SOUND
    };

    /**
     * Device status
     *
     * UNASSIGNED: device detected but not fingerprinted,
     * PENDING_LIST: device is fingerprinted, waiting list add
     * IN_LIST: device is already in list
     *
     */

    public enum STATUS {
        UNASSIGNED,
        PENDING_LIST,
        IN_LIST,
    };

    /**
     * Constructor
     *
     * @param device
     * @param sOwner
     * @param sDevice
     * @param sESN
     * @param lLastSeen
     * @param scanRecord
     * @param iRSSI
     * @param dtType
     */
    public DeviceInfo(BluetoothDevice device, String sOwner, String sDevice, String sESN, long lLastSeen, byte[] scanRecord, int iRSSI, DEVICE_TYPE dtType) {
        mDeviceObj = device;
        mAddress = device.getAddress();
        mOwner = sOwner;
        mDevice = sDevice;
        mESN = sESN;
        mLastSeen = lLastSeen;
        mType = dtType;
        mSR = scanRecord;
        mRSSI = iRSSI;
        mStatus = STATUS.UNASSIGNED;
    }

    /**
     * getAddress
     *
     * Retrieve the MAC address of the device.
     *
     * @return
     */
    public String getAddress() {
        return mAddress;
    }


    /**
     * getOwner
     *
     * Retrieve the owner's info
     *
     * @return
     */
    public String getOwner() {
        return mOwner;
    }


    /**
     * getDevice
     *
     * Retrieve the device name
     *
     * @return
     */
    public String getDevice() {
        return mDevice;
    }


    /**
     * setDevice
     *
     * Set device name.
     *
     * @param device
     */
    public void setDevice(String device){
        mDevice = device;
    }


    /**
     * getObject
     *
     * Retrieve the underlying BluetoothDevice object.
     * @return
     */
    public BluetoothDevice getObject(){
        return mDeviceObj;
    }


    /**
     * getESN
     *
     * Retrieve the Electronic Serial Number of the device
     *
     * @return
     */
    public String getENS() {
        return mESN;
    }

    /**
     * getScanRecord
     *
     * Retrieve the original scan record
     *
     * @return
     */
    public byte[] getScanRecord() { return mSR; }

    /**
     * getRSSI
     *
     * Retrieve the signal power
     *
     * @return
     */
    public int getRSSI() {
        return mRSSI;
    }


    /**
     * Update RSSI
     *
     * Set signal level
     * @param iRSSI
     */
    public void updateRSSI(int iRSSI) {
        mRSSI = iRSSI;
    }


    /**
     * getType
     *
     * Get device type
     *
     * @return
     */
    public DEVICE_TYPE getType() {
        return mType;
    }


    /**
     * setType
     *
     * Set device type.
     *
     * @param type
     */

    public void setType(DEVICE_TYPE type) {
        mType = type;
    }

    public void setLastSeen(long lLastSeen) { mLastSeen = lLastSeen;}

    /**
     * Status related routines.
     */
    public void addedInList(){
        mStatus = STATUS.PENDING_LIST;
    }


    public void foundInUiList(){
        mStatus = STATUS.IN_LIST;
    }


    public boolean isInUiList() {
        return (mStatus == STATUS.IN_LIST);
    }


    public boolean isPendingList(){
        return (mStatus == STATUS.PENDING_LIST);
    }

    /**
     * Accurate fingerprinting related routines
     */
    public void incTries() {
        tries++;
    }


    public int getTries(){
        return tries;
    }
}

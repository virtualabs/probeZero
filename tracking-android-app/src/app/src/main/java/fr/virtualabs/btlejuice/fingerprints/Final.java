package fr.virtualabs.btlejuice.fingerprints;

import android.util.Log;

import fr.virtualabs.btlejuice.Fingerprint;

/**
 * Created by virtualabs on 03/02/16.
 */
public class Final extends Fingerprint {

    public Final(Fingerprint fpNext) {
        super(fpNext);
    }

    /**
     * Consider we were not able to fingerprint the device.
     */

    public void perform() {
        if (getCallback() != null) {
            Log.i("Final", "Error reported");
            getCallback().onError();
        }

    }
}

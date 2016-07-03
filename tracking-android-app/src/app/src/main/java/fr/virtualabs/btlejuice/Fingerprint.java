package fr.virtualabs.btlejuice;

import android.util.Log;

/**
 * Created by virtualabs on 02/02/16.
 */
public class Fingerprint {

    public static class FingerprintCallback {
        public void onSuccess() {
        }

        public void onError() {
        }
    };

    protected FingerprintCallback mCallback = null;
    protected Fingerprint mNext = null;

    public Fingerprint(FingerprintCallback callback) {
        mCallback = callback;
    }

    public Fingerprint(Fingerprint fpNext) {
        mNext = fpNext;
    }

    public Fingerprint(FingerprintCallback callback, Fingerprint fpNext) {
        mCallback = callback;
        fpNext.setCallback(mCallback);
    }

    public void setCallback(FingerprintCallback callback) {
        mCallback = callback;
        if (mNext != null)
            mNext.setCallback(callback);
    }

    public FingerprintCallback getCallback() {
        return mCallback;
    }

    public void setNext(Fingerprint fpNext) {
        mNext = fpNext;
    }

    private Fingerprint getNext() {
        return mNext;
    }

    public void processNext() {
        if (getNext() != null) {
            Log.i("Fingerprint", "Process next: " + mNext.toString());
            getNext().perform();
        }
    }

    public void perform() {
    }
}

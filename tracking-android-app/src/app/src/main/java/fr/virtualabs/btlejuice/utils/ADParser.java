package fr.virtualabs.btlejuice.utils;


import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class ADParser {
    public static class Record {
        private byte mType;
        private byte[] mData;

        public Record(byte type, byte[] data) {
            mType = type;
            mData = data;
        }

        public byte getType(){
            return mType;
        }

        public byte[] getData() {
            return mData;
        }
    }


    public static ArrayList<Record> parse(byte[] recordData) {
        final ArrayList<Record> records = new ArrayList<Record>();
        int i = 0;
        byte type, length;
        byte[] buffer;

        /* Parse recordData. */
        while ((i<recordData.length) && (recordData[i] != 0)) {
            length = recordData[i];
            type = recordData[i+1];
            buffer = Arrays.copyOfRange(recordData, i+2, i+2+length-1);
            records.add(new ADParser.Record(type, buffer));
            Log.i("POF", String.format("Record %02X %02X", length, type));
            i+= (length+1);
        }

        /* Return records. */
        return records;
    }
}

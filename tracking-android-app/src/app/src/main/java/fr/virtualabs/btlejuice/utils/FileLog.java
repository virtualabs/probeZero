package fr.virtualabs.btlejuice.utils;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by virtualabs on 11/02/16.
 */
public class FileLog {

    public static void write(String message) {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath() + "/btlejuice");
        Log.i("FileLog","Logging to "+dir.getAbsolutePath());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "log.txt");

        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
            buf.append(message);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            Log.e("FileLOG", "Cannot write to SD");
            e.printStackTrace();
        }
    }
}

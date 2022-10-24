package cy.org.cyens.reinherit;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogWriter {
    private static final String TAG = LogWriter.class.getName();

    private final String mFilename;
    private FileOutputStream mFileOutputStream = null;
    private final Context mContext;


    public LogWriter(Context context, String filename) {
        this.mContext = context;
        mFilename = filename;

        try {
            mFileOutputStream = new FileOutputStream(createFile(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
/* new code - previous implementation of the createFile function
    @NonNull
    private File createFile() {
        File directory = mContext.getDir(DIRECTORY_NAME, Context.MODE_PRIVATE);

        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "Error creating directory " + directory);
        }

        return new File(directory, mFilename);
    }
*/
    @NonNull
    private File createFile() {
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Reinherit/Logs/");

        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(TAG, "Error creating directory " + directory);
        }

        return new File(directory, mFilename);
    }

    public void appendData(String data) {
        try {
            mFileOutputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flush() {
        try {
            mFileOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            mFileOutputStream.flush();
            mFileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package cy.org.cyens.reinherit;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.core.Point3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

public class LogWriter {
    private static final String TAG = LogWriter.class.getName();

    private static final String DIRECTORY_NAME = "logs";
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
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/ReinheritLogs/");

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

    // 1. Timestamp of the current frame
    // 2. index of tracker
    // 3. 3d coordinates of the person
    // 4. distance from reference point
    public static String dataToString(Date timeFrame, int id, Point3 p, double dist, int NumberOfMusicians) { //change this - added one more variable, NumberOfMusicians
        return String.format(Locale.US, "%s,%d,%.2f,%.2f,%.2f,%.2f,%d", timeFrame.toString(), id, p.x, p.y, p.z, dist, NumberOfMusicians) + "\n";
    }

    public static String rfPositionToString(Date timeFrame, Point3 p){
        return String.format(Locale.US, "%s,%.2f,%.2f,%.2f", timeFrame.toString(), p.x, p.y, p.z) + "\n";
    }
}

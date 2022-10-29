package cy.org.cyens.reinherit;

import android.os.Environment;

import androidx.lifecycle.ViewModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MusiciansTrackingViewModel extends ViewModel {
    private final String logFilePath;
    private static final SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmssSS");
    private final HashMap<String, Integer> mMusiciansTracker;

    public MusiciansTrackingViewModel()
    {
        logFilePath = Environment.getExternalStorageDirectory().getPath() + "/Reinherit/Logs/Musicians_" + mDateFormatter.format(Calendar.getInstance().getTime()) + ".csv";
        File logFile = new File(logFilePath);
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mMusiciansTracker = new HashMap<>();
    }

    public int getMusiciansInArea(String area){
        if (mMusiciansTracker == null || !mMusiciansTracker.containsKey(area))
            return -1;

        Integer returnValue = mMusiciansTracker.get(area);
        return returnValue != null ? returnValue : -1;
    }

    public synchronized void writeAreaData(String area, int count){
        mMusiciansTracker.put(area, count);

        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(logFilePath, true);
            csvWriter.append(String.format(Locale.US, "%s,%s,%d\n", mDateFormatter.format(Calendar.getInstance().getTime()), area, count));
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
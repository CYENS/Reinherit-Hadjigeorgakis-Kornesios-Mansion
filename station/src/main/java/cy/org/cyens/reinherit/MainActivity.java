package cy.org.cyens.reinherit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Scanner;

import cy.org.cyens.common.BluetoothService;
import cy.org.cyens.common.Constants;


public class MainActivity extends AppCompatActivity {
    //region Constants *****************************************************************************
    private static final int BLUETOOTH_REQUEST_CODE = 102;
    private static final int ACCESS_FINE_LOCATION_CODE = 103;
    private static final int ACCESS_COARSE_LOCATION_CODE = 104;
    //endregion Constants **************************************************************************

    private static final int MAX_AUDIO_FILES = 5;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 101;

    // new code
    private static final int MAX_PERSON_COUNTER_HISTORY_SIZE = 8;
    // end new code

    private static final String TAG = MainActivity.class.getName();
    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyyMMdd_HHmmssSS");

    private LogWriter mLogDetectWriter;

    private boolean soundStopped = false;
    private boolean cameraState = true;

    private TextView mTrackerText;
    private TextView mConnectionStatus;

    private EditText mDeviceNameText;

    //new variables
    //define the media player here
    private static MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    //define some log variables here
    private int NumberOfMusicians = 0;
    private int mChangeMetricThreshold = 10;

    private PreviewView mPreviewView;

    private CameraInputManager mCameraManager;
    private SoundManager mSoundManager;
    private BluetoothManager mBluetoothManager;
    //end of new variables

    // Currently Connected device name
    private String mConnectedDeviceName;

    static {
        if (!OpenCVLoader.initDebug())
            Log.e(TAG, "Opencv NOT loaded");
        else
            Log.d(TAG, "Opencv loaded");
    }


    public class PeopleCounterChangeCallback implements CameraInputManager.IPersonCounterChangeCallback
    {
        @Override
        public void OnPersonCounterChange(@NonNull int counter) {
            mSoundManager.playSound(counter);
        }
    }
    private final PeopleCounterChangeCallback mPeopleCounterChangeCallback = new PeopleCounterChangeCallback();

    //region Callback Handlers
    public class MetricsUpdateCallback implements CameraInputManager.IVoidCallback
    {
        private int logsCaptured = 0;

        @Override
        public void invoke() {
            CameraInputManager.MetricsData md = mCameraManager.getMetricsData();
            setStatus(mTrackerText, String.format(Locale.US, "Metric: %d\nAVG: %.1f ( B: %.1f\t, F: %.1f\t, C: %.1f)\nW: %.2f\t (MN: %.0f\tMX: %.0f)", md.getCounterMetric(), md.mGrandMetric, md.mBaseMetric, md.mFlowMetric, md.mChangeMetric,  md.mBaseMetricWeight, md.mMinMetricValue, md.mMaxMetricValue, NumberOfMusicians));
            mLogDetectWriter.appendData(String.format(Locale.US, "%s,%d,%f,%f,%f,%f,%f,%f,%f,%d\n", mDateFormatter.format(Calendar.getInstance().getTime()), md.getCounterMetric(), md.mGrandMetric, md.mBaseMetric, md.mFlowMetric, md.mChangeMetric, md.mBaseMetricWeight, md.mMinMetricValue, md.mMaxMetricValue, NumberOfMusicians));
            logsCaptured++;
            if (logsCaptured == 100)
            {
                mLogDetectWriter.flush();
                logsCaptured = 0;
            }
        }
    }
    private final MetricsUpdateCallback mMetricsUpdatedCallback = new MetricsUpdateCallback();

    public class BasePictureResetCallback implements CameraInputManager.IVoidCallback
    {
        @Override
        public void invoke() {
            CameraInputManager.MetricsData md2 = mCameraManager.getMetricsData();
            mLogDetectWriter.appendData(String.format(Locale.US, "%s,%d,%f,%f,%f,%f,%f,%f,%f,%d\n", mDateFormatter.format(Calendar.getInstance().getTime()), -1, -1.0, -1.0, -1.0, -1.0,  md2.mBaseMetricWeight, md2.mMinMetricValue, md2.mMaxMetricValue, NumberOfMusicians));
        }
    }
    private final BasePictureResetCallback mBasePictureResetCallback = new BasePictureResetCallback();

    public class OnTriggerScreenshotCaptureCallback implements CameraInputManager.IVoidCallback
    {
        @Override
        public void invoke() {
        }
    }
    private final OnTriggerScreenshotCaptureCallback nOnTriggerScreenshotCaptureCallback = new OnTriggerScreenshotCaptureCallback();
    //endregion Callback Handlers

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.camera_permission_granted, Toast.LENGTH_LONG).show();
                mCameraManager.initializeCamera();
            } else {
                Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.ex_storage_permission_granted, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.ex_storage_permission_denied, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == BLUETOOTH_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_bluetooth_granted,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.permission_bluetooth_denied,
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == ACCESS_FINE_LOCATION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_location_fine_granted,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.permission_location_fine_denied,
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == ACCESS_COARSE_LOCATION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_location_coarse_granted,
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.permission_location_coarse_denied,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        File dataDir = new File(Environment.getExternalStorageDirectory().getPath() + "/Reinherit/");
        File logDir = new File(dataDir.getPath() + "/Logs/");
        if (!logDir.exists()){
            logDir.mkdirs();
        }
        File imagesDir = new File(dataDir.getPath() + "/Images/");
        if (!imagesDir.exists()){
            imagesDir.mkdirs();
        }
        mPreviewView = this.findViewById(R.id.viewFinder);
        mDeviceNameText = (EditText)findViewById(R.id.editDeviceName);

        // Required permissions for camera access
        mCameraManager = new CameraInputManager(this);
        mCameraManager.setOutputDir(imagesDir.getAbsolutePath() + "/");
        readWeight();
        mCameraManager.getMetricsData().setMaxCounterValue(MAX_PERSON_COUNTER_HISTORY_SIZE);
        mCameraManager.setPersonCounterChangeCallback(mPeopleCounterChangeCallback);
        mCameraManager.setMetricsUpdateCallback(mMetricsUpdatedCallback);
        mCameraManager.setBasePictureResetCallback(mBasePictureResetCallback);
        mCameraManager.setTriggerScreenshotCaptureCallback(nOnTriggerScreenshotCaptureCallback);
        mCameraManager.setChangeThreshold(mChangeMetricThreshold);

        mSoundManager = new SoundManager(MAX_AUDIO_FILES, "/Reinherit/", "sound", "aif", this, true, true);

        // Required permissions for external storage management
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // for android sdk >=30
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, CAMERA_REQUEST_CODE);
        }

        // Get status view by id
        mTrackerText = findViewById(R.id.textViewTracker);
        mConnectionStatus = findViewById(R.id.textViewConnectionStatus);

        addUiListeners();

        mBluetoothManager = new BluetoothManager(this);
        mDeviceNameText = (EditText)findViewById(R.id.editDeviceName);

        readDeviceName(); //read the device name from the txt here

        // Ensure device is discoverable by others
        mBluetoothManager.ensureDiscoverable();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        mBluetoothManager.onStart(this, mHandler);

        // TODO: ensure app has permissions before retrieving device name
        String deviceID = mDeviceNameText.getText().toString();
        try {
            deviceID = BluetoothAdapter.getDefaultAdapter().getName();
            ((TextView) findViewById(R.id.textViewDeviceID)).setText(deviceID);
        }catch(Exception e) {
            Log.w(TAG, e.getMessage());
        }

        createNewLogWriter();
    }

    @Override
    public void onResume() {
        super.onResume();
        mBluetoothManager.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSoundManager.onDestroy();
        mBluetoothManager.onDestroy();
    }

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            mBluetoothManager.setName("n" + String.valueOf(mDeviceNameText.getText()));//new code below
                            mBluetoothManager.sendDeviceNameOverBluetooth();
                            setStatus(mConnectionStatus, getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(mConnectionStatus, R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(mConnectionStatus, R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ: // handle received commands
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessageStr = new String(readBuf, 0, msg.arg1);
                    JSONObject readMessageJson;

                    int valueInt = 0;
                    //new code to get the double
                    double valueDouble =0;
                    Constants.COMMANDS id;
                    try {
                        readMessageJson = new JSONObject(readMessageStr);
                        id = Constants.COMMANDS.fromString(readMessageJson.getString("id"));
                        if (readMessageJson.has("val")){
                            valueInt = readMessageJson.getInt("val");
                            valueDouble = readMessageJson.getDouble("val");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }

                    // Check the id of the message and determine type
                    switch (id) {
                        case GET_STATUS:
                            //mService.write(mStatus.getText().toString().getBytes()); <--new code follows. Uncomment this if needed
                            mTrackerText = findViewById(R.id.textViewTracker);
                            String textToSend = "s"+mTrackerText.getText();
                            mBluetoothManager.write(textToSend.getBytes());
                            //end of new code

                            ImageUtilities.saveBitmap(mPreviewView.getBitmap(), "gs_");
                            break;
                        case RESET_CAMERA_POSE:
                            break;
                        case SET_FREQ:
                            Slider slider = findViewById(R.id.sliderFreq); //even more new code here
                            slider.setValue(valueInt);
                            break;
                        case START:
                            soundStopped = false;
                            break;
                        case STOP:
                            mSoundManager.stopSound(0);
                            mCameraManager.resetPersonCounterData();
                            soundStopped = true;
                            break;
                        //new code here
                        case PLAY_SOUND:
                            if(isPlaying){
                                mediaPlayer.stop();
                                mediaPlayer.release();
                                isPlaying = false;
                            }else{
                                String soundPath = Environment.getExternalStorageDirectory().getPath() + "/Reinherit/sound1.wav";
                                File file = new File(soundPath);//maybe this is not needed
                                if(file.exists()){
                                    mediaPlayer = MediaPlayer.create(MainActivity.this, Uri.parse(soundPath));
                                    mediaPlayer.start();
                                    mediaPlayer.setLooping(true);
                                    isPlaying = true;
                                }else{
                                    break;
                                }
                            }
                            break;
                        case  CAMERA_DISPLAY:
                            togglePreviewVisibility();
                            break;
                        case RAISE_VOLUME:
                            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                            audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
                            break;
                        case LOWER_VOLUME:
                            AudioManager audioManager2 = (AudioManager) getSystemService(AUDIO_SERVICE);
                            audioManager2.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
                            break;
                        case SET_MUSICIANS:
                            NumberOfMusicians = (int)valueInt;
                            CameraInputManager.MetricsData md = mCameraManager.getMetricsData();
                            mLogDetectWriter.appendData(String.format(Locale.US, "%s,%d,%f,%f,%f,%f,%f,%f,%f,%d\n", mDateFormatter.format(Calendar.getInstance().getTime()), -1, -1.0, -1.0, -1.0, -1.0,  md.mBaseMetricWeight, md.mMinMetricValue, md.mMaxMetricValue, NumberOfMusicians));
                            break;
                        case SET_MAX_VALUE:
                            mCameraManager.getMetricsData().setMaxValue((int) valueInt);
                            saveMetricsData();
                            try {
                                UpdateWeightsCSV();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case SET_MIN_VALUE:
                            mCameraManager.getMetricsData().setMinValue((int) valueInt);
                            saveMetricsData();
                            try {
                                UpdateWeightsCSV();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case SET_WEIGHT:
                            mCameraManager.getMetricsData().setBaseFlowWeight((float) valueDouble);
                            saveMetricsData();
                            try {
                                UpdateWeightsCSV();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case SET_BASE_IMAGE:
                            mCameraManager.resetBaseFrame();
                            ImageUtilities.takeScreenshot(mPreviewView, "bi");
                            CameraInputManager.MetricsData md2 = mCameraManager.getMetricsData();
                            mLogDetectWriter.appendData(String.format(Locale.US, "%s,%d,%f,%f,%f,%f,%f,%f,%f,%d\n", mDateFormatter.format(Calendar.getInstance().getTime()), -1, -1.0, -1.0, -1.0, -1.0,  md2.mBaseMetricWeight, md2.mMinMetricValue, md2.mMaxMetricValue, NumberOfMusicians));
                            break;
                        case RESET_LOG:
                            createNewLogWriter();
                            break;
                    }

                    Log.d(TAG, readMessageStr);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Log.d(TAG, "Connected to " + mConnectedDeviceName);
                    break;
                case Constants.MESSAGE_TOAST:
                    Log.d(TAG, msg.getData().getString(Constants.TOAST));
                    break;
            }
        }
    };

    private void togglePreviewVisibility(){
        int viewVisibility = mPreviewView.getVisibility();
        mPreviewView.setVisibility((viewVisibility == View.VISIBLE) ? View.INVISIBLE : View.VISIBLE);
    }

    private void addUiListeners() {

        mDeviceNameText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveMetricsData();
                return true;
            }
            return false;
        });

        Button button = findViewById(R.id.startStopButton);
        button.setOnClickListener(v -> {
            if (soundStopped) {
                mSoundManager.stopSound(0);
            } else {
                mSoundManager.stopSound(0);
            }
            soundStopped = !soundStopped;
        });

        Button btn2 = findViewById(R.id.TurnOnOffCamera);
        btn2.setOnClickListener(v -> {
            togglePreviewVisibility();
        });

        //new code
        Button btn3 = findViewById(R.id.saveImages);
        btn3.setOnClickListener(v -> {
            ImageUtilities.saveBitmap(mPreviewView.getBitmap(), "db-pv");

            mCameraManager.captureDebugImages("db");
            /*
            if (isPlaying) {

                mediaPlayer.stop();
                mediaPlayer.release();
                isPlaying = false;

            } else {

                String soundPath = Environment.getExternalStorageDirectory().getPath() + "/Reinherit/sound1.wav";
                File file = new File(soundPath);//maybe this is not needed
                if(file.exists()) {
                    mediaPlayer = MediaPlayer.create(MainActivity.this, Uri.parse(soundPath));
                    mediaPlayer.start();
                    mediaPlayer.setLooping(true);
                    isPlaying = true;
                }
            }
            */
        });

        Button btn4 = findViewById(R.id.setBaseImage);
        btn4.setOnClickListener(v -> {
            mCameraManager.resetBaseFrame();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Set status text field
    private void setStatus(TextView status, int stringId) {
        runOnUiThread(() -> status.setText(getString(stringId)));
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(TextView status, CharSequence subTitle) {
        runOnUiThread(() -> status.setText(subTitle));
    }

    private boolean readDeviceName(){
        try {
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/Reinherit/Logs/Weight.txt"; //file path
            File myObj = new File(filePath);

            if(!myObj.exists()) return false;

            Scanner myReader = new Scanner(myObj);
            //We don't want to read the whole text file just the first line
            int counter = 0;
            while (myReader.hasNextLine()) {
                if(counter == 3){
                    mDeviceNameText.setText(myReader.nextLine());
                    break;
                }
                myReader.nextLine();
                counter++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            mDeviceNameText.setText("NoNameDevice");
            e.printStackTrace();
        }
        return true;
    }

    private void createNewLogWriter() {
        if (mLogDetectWriter != null){
            mLogDetectWriter.close();
        }

        String deviceID = mDeviceNameText.getText().toString();
        mLogDetectWriter = new LogWriter(getApplicationContext(), "log_" + deviceID + "_" + mDateFormatter.format(Calendar.getInstance().getTime()) + ".csv");
        mLogDetectWriter.appendData("timestamp,people,total,base,flow,change,base-weight,min,max,musicians\n");
        CameraInputManager.MetricsData md = mCameraManager.getMetricsData();
        mLogDetectWriter.appendData(String.format(Locale.US, "%s,%d,%f,%f,%f,%f,%f,%f,%f,%d\n", mDateFormatter.format(Calendar.getInstance().getTime()), 1, -1.0, -1.0, -1.0, -1.0,  md.mBaseMetricWeight, md.mMinMetricValue, md.mMaxMetricValue, NumberOfMusicians));
    }

    // region IO
    private void saveMetricsData(){
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/Reinherit/Logs/Weight.txt";
        try {
            File myObj = new File(filePath);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        try {
            FileWriter myWriter = new FileWriter(filePath);
            CameraInputManager.MetricsData md = mCameraManager.getMetricsData();
            myWriter.append(String.format(Locale.US, "%f\n%f\n%f\n%s\n", md.mBaseMetricWeight, md.mMinMetricValue, md.mMaxMetricValue, mDeviceNameText.getText().toString()));
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void readWeight(){

        try {
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/Reinherit/Logs/Weight.txt"; //file path

            File myObj = new File(filePath);
            Scanner myReader = new Scanner(myObj);

            int counter = 0;
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                switch (counter){
                    case 0:
                        mCameraManager.getMetricsData().setBaseFlowWeight(Float.parseFloat(data));
                        break;
                    case 1:
                        mCameraManager.getMetricsData().setMinValue(Double.parseDouble(data));
                        break;
                    case 2:
                        mCameraManager.getMetricsData().setMaxValue(Double.parseDouble(data));
                        break;
                }
                counter++;

                if(counter == 3)
                    break; //get out of the while, the last value is the name of the device

                System.out.println(data);
            }

            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

    }

    private void UpdateWeightsCSV() throws IOException {
        FileWriter csvWriter = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/Reinherit/Logs/WeightsFile.csv", true);
        CameraInputManager.MetricsData md = mCameraManager.getMetricsData();
        csvWriter.append(String.format(Locale.US, "%s,%f,%f,%f\n", mDeviceNameText.getText(),md.mBaseMetricWeight, md.mMinMetricValue, md.mMaxMetricValue));
        csvWriter.flush();
        csvWriter.close();
    }
    //endregion IO
}

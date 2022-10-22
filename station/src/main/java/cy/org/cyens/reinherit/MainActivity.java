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
import android.view.KeyEvent;
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
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.Scanner;

import cy.org.cyens.common.BluetoothService;
import cy.org.cyens.common.Constants;


public class MainActivity extends AppCompatActivity {
    private static final int MAX_AUDIO_FILES = 5;
    private static final boolean USE_SQUARE_FRAME = false;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private static final int BLUETOOTH_REQUEST_CODE = 102;
    private static final int ACCESS_FINE_LOCATION_CODE = 103;
    private static final int ACCESS_COARSE_LOCATION_CODE = 104;

    private static final int REQUEST_ENABLE_BT = 3;

    private static final int CAMERA_INDEX = 0;
    private static final int PROCESSING_FRAME_SIZE = 300; // px
    // new code
    private static final int FLOW_HEIGHT = 360; // px 540 432 360
    private static final int FLOW_WIDTH = 640; // px  960 768 640

    private static final int MAX_PERSON_COUNTER_HISTORY_SIZE = 8;
    // end new code

    private static final int FRAME_CALL_GC = 200;
    private static final String TAG = MainActivity.class.getName();
    private static final Size FLOW_FRAME_SIZE = new Size(FLOW_WIDTH, FLOW_HEIGHT);
    private static final String PATCH_FILENAME = "patch.pd";

    private final Mat frameProcessed = new Mat();
    private int frameCounter = 0;

    private LogWriter mLogDetectWriter;

    private boolean soundStopped = false;
    private boolean cameraState = true;

    private TextView mTrackerText;
    private TextView mConnectionStatus;

    //new variables
    //define the media player here
    private static MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    //define some log variables here
    private int NumberOfMusicians = 0;
    //flag to capture a frame of the camera into a two dimensional matrix
    private boolean CaptureFrame = false;

    private static String BluetoothName;

    private Mat BaseFrame = null;

    private Mat LastFrame = null;

    private static final double noise_value = 10;

    private double weight_base, weight_flow, base_sum;

    private double minValue = 5;

    private double maxValue = 60;

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.camera_permission_granted, Toast.LENGTH_LONG).show();
                mCameraManager.initializeCamera(this);
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

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        mBluetoothManager.onStart(this, mHandler);
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
                            EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName);
                            BluetoothName = String.valueOf(DeviceNameText.getText());//new code below
                            BluetoothName = "n"+BluetoothName;
                            mBluetoothManager.write(BluetoothName.getBytes());
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

                    int value = 0;
                    //new code to get the double
                    double value2 =0;
                    Constants.COMMANDS id;
                    try {
                        readMessageJson = new JSONObject(readMessageStr);
                        id = Constants.COMMANDS.fromString(readMessageJson.getString("id"));
                        if (readMessageJson.has("val")){
                            value = readMessageJson.getInt("val");
                            //new code here also to save the double
                            value2 = readMessageJson.getDouble("val");
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

                            //takeSc(getWindow().findViewById(R.id.viewFinder));//new code under all that
                            /*
                            ByteArrayOutputStream stream=new ByteArrayOutputStream();
                            sc.compress(Bitmap.CompressFormat.PNG,100, stream);
                            byte[] imageBytes = stream.toByteArray();
                            int subArraySize=400;

                            mService.write(String.valueOf(imageBytes.length).getBytes());

                            for(int i=0;i<imageBytes.length;i+=subArraySize){
                                byte[] tempArray;
                                tempArray= Arrays.copyOfRange(imageBytes,i,Math.min(imageBytes.length,i+subArraySize));
                                mService.write(tempArray);
                            }

                             */

                            break;
                        case RESET_CAMERA_POSE:
                            break;
                        case SET_FREQ:
                            //PdBase.sendFloat("freq", Math.max(0, value));
                            Slider slider = findViewById(R.id.sliderFreq); //even more new code here
                            slider.setValue(value);
                            break;
                        case START:
                            //PdBase.sendFloat("start", 1.0f);
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
                            NumberOfMusicians = (int)value;
                            mLogDetectWriter.appendData(String.format(Locale.US, "%s,%d,%f,%f,%f,%f,%f,%f,%d\n", Calendar.getInstance().getTime(), -1, -1.0, -1.0, -1.0,  weight_base, minValue, maxValue, NumberOfMusicians));
                            break;
                        case SET_MAX_VALUE:
                            maxValue = (int)value;
                            saveWeight();
                            break;
                        case SET_MIN_VALUE:
                            minValue = (int)value;
                            saveWeight();
                            break;
                        case SET_WEIGHT:
                            weight_base= value2;
                            saveWeight();
                            break;
                        case SET_BASE_IMAGE:
                            CaptureFrame = true;
                            try {
                                UpdateWeightsCSV();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readWeight();
        readDeviceName(); //read the device name from the txt here

        mPreviewView = this.findViewById(R.id.viewFinder);

        // Required permissions for camera access
        mCameraManager = new CameraInputManager();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
        else {
            mCameraManager.initializeCamera(this);
        }

        mSoundManager = new SoundManager(MAX_AUDIO_FILES, "/Reinherit/", "sound", "wav", this);

        // Required permissions for bluetooth access
        // Android >=10 requires location permissions for bluetooth scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_REQUEST_CODE);
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, ACCESS_COARSE_LOCATION_CODE);
        }

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

        String deviceID = BluetoothAdapter.getDefaultAdapter().getName();
        ((TextView) findViewById(R.id.textViewDeviceID)).setText(deviceID);

        mLogDetectWriter = new LogWriter(getApplicationContext(), deviceID + "_log.csv");
        mLogDetectWriter.appendData(String.format(Locale.US, "%s,%d,%f,%f,%f,%f,%f,%f,%d\n", Calendar.getInstance().getTime(), 1, -1.0, -1.0, -1.0,  weight_base, minValue, maxValue, NumberOfMusicians));

        // Get status view by id
        mTrackerText = findViewById(R.id.textViewTracker);
        mConnectionStatus = findViewById(R.id.textViewConnectionStatus);

        addUiListeners();

        mBluetoothManager = new BluetoothManager();
        mBluetoothManager.onCreate(this);

        // Ensure device is discoverable by others
        mBluetoothManager.ensureDiscoverable();
    }

    private void togglePreviewVisibility(){
        int viewVisibility = mPreviewView.getVisibility();
        mPreviewView.setVisibility((viewVisibility == View.VISIBLE) ? View.INVISIBLE : View.VISIBLE);
    }

    private void addUiListeners() {

        EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName); //get the text UI
        DeviceNameText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //saveDeviceName();
                    saveWeight();
                    return true;
                }
                return false;
            }
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
        Button btn3 = findViewById(R.id.DefaultSoundOnOff);
        btn3.setOnClickListener(v -> {
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
        });

        Button btn4 = findViewById(R.id.setBaseImage);
        btn4.setOnClickListener(v -> {
            CaptureFrame = true;
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

    private void readDeviceName(){
        try {

            //String filePath = Environment.getExternalStorageDirectory().getPath() + "/ReinheritLogs/device_name.txt"; //file path
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/ReinheritLogs/Weight.txt"; //file path

            File myObj = new File(filePath);
            Scanner myReader = new Scanner(myObj);
            EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName); //get the view ID for the text UI
            //DeviceNameText.setText(myReader.nextLine());

            //We don't want to read the whole text file just the first line

            int counter = 0;

            while (myReader.hasNextLine()) {
                if(counter == 3){
                    DeviceNameText.setText(myReader.nextLine());
                    break;
                }
                myReader.nextLine();
                counter++;
                //String data = myReader.nextLine();
                //System.out.println(data);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName); //get the view ID for the text UI
            DeviceNameText.setText("NoNameDevice");
            e.printStackTrace();
        }
    }

    // region IO
    private void saveWeight(){
        String filePath = Environment.getExternalStorageDirectory().getPath() + "/ReinheritLogs/Weight.txt"; //file path

        String weight_to_string = String.valueOf(weight_base);
        String min_value_to_string = String.valueOf(minValue);
        String max_value_to_string = String.valueOf(maxValue);
        String deviceName; //define a string to keep the name here
        EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName); //get the text UI
        deviceName = String.valueOf(DeviceNameText.getText()); //assign the value


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
            myWriter.append(weight_to_string + "\n"); //weight
            myWriter.append(min_value_to_string + "\n"); //min value
            myWriter.append(max_value_to_string + "\n"); //max value
            myWriter.append(deviceName); //device name
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void readWeight(){

        try {
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/ReinheritLogs/Weight.txt"; //file path

            File myObj = new File(filePath);
            Scanner myReader = new Scanner(myObj);

            int counter = 0;
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                switch (counter){
                    case 0:
                        weight_base = Double.parseDouble(data);
                        break;
                    case 1:
                        minValue = Double.parseDouble(data);
                        break;
                    case 2:
                        maxValue = Double.parseDouble(data);
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

        EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName);

        FileWriter csvWriter = new FileWriter(Environment.getExternalStorageDirectory().getPath() + "/ReinheritLogs/WeightsFile.csv", true);
        csvWriter.append(String.format("%s,%f,%f,%f\n" ,DeviceNameText.getText(),weight_base, minValue, maxValue));
        csvWriter.flush();
        csvWriter.close();
    }
    //endregion IO
}

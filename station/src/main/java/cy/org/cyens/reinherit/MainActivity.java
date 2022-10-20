package cy.org.cyens.reinherit;

import static org.opencv.imgproc.Imgproc.INTER_AREA;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap; // We need this for the screenshots
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.SoundPool;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.slider.Slider;
import com.google.ar.core.ImageFormat;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

//new imports for sounds here
import android.media.MediaPlayer;

import cy.org.cyens.common.BluetoothService;
import cy.org.cyens.common.Constants;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
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
    private static final Size FLOW_FRAME_SIZE = new Size(FLOW_WIDTH, FLOW_HEIGHT);
    private static final String TAG = MainActivity.class.getName();
    private static final String PATCH_FILENAME = "patch.pd";
    private static final int MAX_AUDIO_FILES = 5;

    private final Mat frameProcessed = new Mat();
    private int frameCounter = 0;
    private Mat frame = null;
    private int frameSize;
    private float processingScaling; // assuming frame is square thus same scaling on x and y axis

    private boolean clicked = false;
    private boolean setReferencePoint = false;

    private Calibrator mCalibrator;
    private LogWriter mLogDetectWriter;
    private LogWriter mLogRFWriter;

    private boolean visualizeResults = false;

    private boolean soundStopped = false;
    private boolean cameraState = true;

    private TextView mStatus;
    private TextView mTrackerText;
    private TextView mConnectionStatus;

    private JavaCamera2Frame javaCameraFrame;
    //new variables
    //define the media player here
    private static MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    private SoundPool soundPool;
    private int[] mSoundIds;
    private int[] mStreamIds;
    private boolean[] mIsSoundPlaying;

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

    /*
     Count detected humans that are close to reference point on the last captured frame
     */
    private int lastPersonCounter = 0;
    private int[] personCounterHistory;
    private int nextPersonCounterHistoryIdx = 0;
    private int personCounterHistorySum = 0;
    private int personCounterHistoryCount = 0;
    
    private static final String APP_NAME = "Reinherit"; //BT STUFF
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    //end of new variables


    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;
    /**
     * Member object for the services
     */
    private BluetoothService mService = null;

    // Currently Connected device name
    private String mConnectedDeviceName;

    static {
        if (!OpenCVLoader.initDebug())
            Log.d(TAG, "Opencv NOT loaded");
        else
            Log.d(TAG, "Opencv loaded");
    }

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        PdAudio.initAudio(sampleRate, 0, 2, 8, true);
        PdUiDispatcher dispatcher = new PdUiDispatcher();
        //PdBase.setReceiver(dispatcher);
    }

    private void loadPDPatch() throws IOException {
        File pdPatch = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Reinherit/", PATCH_FILENAME);
        //PdBase.openPatch(pdPatch);
    }

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.camera_permission_granted, Toast.LENGTH_LONG).show();
                startCamera();
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

    private void initializeCamera(){

        previewView = findViewById(R.id.viewFinder);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

        javaCameraFrame = new JavaCamera2Frame(null);
    }

    private void startCamera(){
        CaptureFrame = true;
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new android.util.Size(1920, 1080))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (imageProxy == null) return;

                try (@SuppressLint("UnsafeOptInUsageError") Image frameImage = imageProxy.getImage()) {
                    if (frameImage == null) return;
                    javaCameraFrame.update(frameImage);
                    runCalibrator(javaCameraFrame);
                }

                imageProxy.close();
            }
        });

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera inputCamera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis, preview);
        inputCamera.getCameraControl().cancelFocusAndMetering();
    }

    @Override
    public void onResume() {
        super.onResume();
        //PdAudio.startAudio(this);

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth services
                mService.start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //PdAudio.stopAudio();
    }

    @Override
    public void onDestroy() { // TODO puredata and camera
        super.onDestroy();
        for (int i = 0 ; i < mSoundIds.length ; i++) {
            if (mIsSoundPlaying[i])
                soundPool.stop(mStreamIds[i]);

            soundPool.unload(mSoundIds[i]);
        }

        soundPool.release();

        if (mService != null) {
            mService.stop();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onStart() {
        super.onStart();
        if (mBluetoothAdapter == null) {
            return;
        }
        // If BT is not on, request that it be enabled.
        // setup() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the session
        } else if (mService == null) {
// Initialize the BluetoothService to perform bluetooth connections
            mService = new BluetoothService(this, mHandler);
        }
    }


    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName);
                            BluetoothName = String.valueOf(DeviceNameText.getText());//new code below
                            BluetoothName = "n"+BluetoothName;
                            mService.write(BluetoothName.getBytes());
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
                            mService.write(textToSend.getBytes());
                            //end of new code

                            saveBitmap(previewView.getBitmap(), "gs_");

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
                            if (USE_SQUARE_FRAME) {
                                mCalibrator.resetCameraPose();
                                clicked = true;
                            }
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
                            StopSound(0);

                            nextPersonCounterHistoryIdx = 0;
                            personCounterHistorySum = 0;
                            personCounterHistoryCount = 0;

                            //PdBase.sendFloat("stop", 1.0f);
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
                            //}
                            break;
                        case  CAMERA_DISPLAY:
                            CameraBridgeViewBase mOpenCvCameraView = findViewById(R.id.CameraView);
                            if (cameraState) {
                                mOpenCvCameraView.setAlpha(0);
                                cameraState = false;
                            } else {
                                mOpenCvCameraView.setAlpha(1);
                                cameraState = true;
                            }
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

                            if (USE_SQUARE_FRAME) {
                                mLogDetectWriter.appendData(LogWriter.dataToString(Calendar.getInstance().getTime(), -2, mCalibrator.getRefPoint(), 0, NumberOfMusicians));
                            }
                            else {
                                mLogDetectWriter.appendData(String.format("%s,%d,%f,%f,%f,%f,%f,%f,%d\n", Calendar.getInstance().getTime(), -1, -1.0, -1.0, -1.0,  weight_base, minValue, maxValue, NumberOfMusicians));
                            }
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
        //if(weight_base == -99)
        //weight_base = WEIGHT_BASE;
        readWeight();

        readDeviceName(); //read the device name from the txt here


        // Required permissions for camera access
        initializeCamera();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
        else {
            startCamera();
        }

        // Required permissions for bluetooth access
        // Android >=10 requires location permissions for bluetooth scan
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_REQUEST_CODE);
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        ACCESS_FINE_LOCATION_CODE);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    ACCESS_COARSE_LOCATION_CODE);
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

        // Setup camera listener.
        CameraBridgeViewBase mOpenCvCameraView = findViewById(R.id.CameraView);

        // Set maximum frame size (finds the closest supported resolution)
        //
        // maximum frame size set by setMaxFrameSize() is bounded by the size of the
        // viewing surface, which could be less than the supported one.
        // thus it does not guarantee that the frameSize will be the one found from
        // getMinSqrResolution() and the image to be square. As a temporary fix, lines
        // (CameraBridgeViewBase.java) 478 & 479 were commented out. In case the frame is larger
        // than the viewing surface, it is automatically scaled to fit the viewing surface
        // at lines (CameraBridgeViewBase.java) 416.

        String deviceID = BluetoothAdapter.getDefaultAdapter().getName();
        ((TextView) findViewById(R.id.textViewDeviceID)).setText(deviceID);

        mLogDetectWriter = new LogWriter(getApplicationContext(), deviceID + "_log.csv");
        mLogRFWriter = new LogWriter(getApplicationContext(), deviceID + "_rf_coordinates.csv");

        mLogRFWriter.appendData(LogWriter.rfPositionToString(Calendar.getInstance().getTime(), new Point3(0,0,0)));
        //mLogDetectWriter.appendData(LogWriter.dataToString(Calendar.getInstance().getTime(), -1, new Point3(0,0,0), 0, NumberOfMusicians));
        mLogDetectWriter.appendData(String.format("%s,%d,%f,%f,%f,%f,%f,%f,%d\n", Calendar.getInstance().getTime(), 1, -1.0, -1.0, -1.0,  weight_base, minValue, maxValue, NumberOfMusicians));

        // Get status view by id
        mStatus = findViewById(R.id.textViewStatusDetails);
        mTrackerText = findViewById(R.id.textViewTracker);
        mConnectionStatus = findViewById(R.id.textViewConnectionStatus);

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

        // Setup PureData
        try {// Initialize PureData
            initPD();
            loadPDPatch();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            finish();
        }
        Button button = findViewById(R.id.startStopButton);
        button.setOnClickListener(v -> {
            if (soundStopped) {
                StopSound(0);
            } else {
                StopSound(0);
            }
            soundStopped = !soundStopped;
        });

        addButtonListeners();

        // Initialize sound pool and load sound files
        AudioAttributes
                audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        soundPool = new SoundPool.Builder().setMaxStreams(MAX_AUDIO_FILES).setAudioAttributes(audioAttributes).build();

        mIsSoundPlaying = new boolean[MAX_AUDIO_FILES];
        mSoundIds = new int[MAX_AUDIO_FILES];
        mStreamIds = new int[MAX_AUDIO_FILES];
        for (int i = 0 ; i < MAX_AUDIO_FILES; i++) {
            String soundPath = Environment.getExternalStorageDirectory().getPath() + "/Reinherit/sound" + (i+1) + ".wav";
            File soundFile = new File(soundPath);
            if (soundFile.exists()){
                mSoundIds[i] = soundPool.load(soundPath, 1);
            }
            mStreamIds[i] = 0;
            mIsSoundPlaying[i] = false;
        }

        personCounterHistory = new int[MAX_PERSON_COUNTER_HISTORY_SIZE];
        for (int i = 0; i < MAX_PERSON_COUNTER_HISTORY_SIZE; i++)
            personCounterHistory[i] = 0;

        // Setup Bluetooth
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
        // Ensure device is discoverable by others
        ensureDiscoverable();
    }

    private void togglePreviewVisibility(){
        int viewVisibility = previewView.getVisibility();
        previewView.setVisibility((viewVisibility == View.VISIBLE) ? View.INVISIBLE : View.VISIBLE);
    }

    private void addButtonListeners() {
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

    /**
     * Makes this device discoverable for 120 seconds. Make it for more
     */
    @SuppressLint("MissingPermission")
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 360);
            startActivity(discoverableIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem visualizeToggle = menu.findItem(R.id.visualize_results);
        visualizeToggle.setTitle(visualizeResults ? R.string.disable_visualize_results : R.string.enable_visualize_results);
        return super.onPrepareOptionsMenu(menu);
    }

    public void onCameraViewStarted(int width, int height) {
        CameraBridgeViewBase mOpenCvCameraView = findViewById(R.id.CameraView);

        Button btn2 = findViewById(R.id.TurnOnOffCamera);
        btn2.setOnClickListener(v -> {
            if (cameraState) {
                mOpenCvCameraView.setAlpha(0);
                cameraState = false;
            } else {
                mOpenCvCameraView.setAlpha(1);
                cameraState = true;
            }
//BT STUFF
            //initiate server
            //ServerClass serverClass = new ServerClass();
//            serverClass.start();
            ///cameraState = !cameraState;
            //mOpenCvCameraView.setVisibility(cameraState);

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
            try {
                UpdateWeightsCSV();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        //end new code
    }

    public Mat onCameraFrame(@NonNull CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // For onCameraFrame, you have to return Mat of the same dimensions as the inputFrame.
        // if flipped it throws an exception if not square
        // https://stackoverflow.com/questions/37970551/opencv-resize-failing
        // https://stackoverflow.com/questions/43547221/change-image-resolution-in-android-with-opencv
        frameCounter++;

        if (frameCounter % FRAME_CALL_GC == 0) {
            // fix opencv memory leak by manual garbage collection
            // https://stackoverflow.com/questions/21050499/memory-leak-from-iterating-opencv-frames
            System.gc();
            System.runFinalization();
            Log.d(TAG, "Garbage Collection");
        }

        return runCalibrator(inputFrame);
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

    // Camera calibration and pose estimation
    private Mat runCalibrator(@NonNull CameraBridgeViewBase.CvCameraViewFrame inputFrame) { //we change this function here
        // Get a new frame
        frame = inputFrame.gray();

        // Rotate frame based on camera
        if (CAMERA_INDEX == 0) { // Back
            Core.rotate(frame, frame, 0);
        } else { // Front
            Core.rotate(frame, frame, 2);
            Core.flip(frame, frame, 1);
        }

        //setup the weight base, weight motion and base sum here at the beginning
        //weight_base = WEIGHT_BASE; <-- set this on create maybe
        weight_flow = 1 - weight_base;
        //base_sum = base_sum

        frame.copyTo(frameProcessed);

        //Imgproc.resize(frame, frameProcessed, FLOW_FRAME_SIZE, 0, 0, INTER_AREA);
        //*new code
        if(CaptureFrame){
            BaseFrame = frameProcessed.clone();
            LastFrame = frameProcessed.clone();
            CaptureFrame = false;
            StopSound(0);

            saveBitmap(previewView.getBitmap(), "pv_");
            //saveMatToBitmap(inputFrame.rgba(), "rgba_");
        }
        if(BaseFrame != null) {

            Mat diffImage = new Mat();
            Core.absdiff(frameProcessed, BaseFrame, diffImage);
            double base_sum = Core.mean(diffImage).val[0];

            Core.absdiff(frameProcessed, LastFrame, diffImage);
            double flow_sum = Core.mean(diffImage).val[0];

            double final_sum = weight_base * base_sum + weight_flow * flow_sum;

            int currentPersonCounter;
            if (final_sum < minValue)
                currentPersonCounter = 0;
            else if (final_sum >= maxValue)
                currentPersonCounter = 5;
            else{
                currentPersonCounter = (int) calculateFloor(final_sum);
            }

            if (personCounterHistoryCount == MAX_PERSON_COUNTER_HISTORY_SIZE)
                personCounterHistorySum -= personCounterHistory[nextPersonCounterHistoryIdx];
            else
                personCounterHistoryCount++;

            personCounterHistory[nextPersonCounterHistoryIdx] = currentPersonCounter;
            personCounterHistorySum += currentPersonCounter;
            nextPersonCounterHistoryIdx = (nextPersonCounterHistoryIdx + 1) % MAX_PERSON_COUNTER_HISTORY_SIZE;

            int averageCurrentPersonCounter = (int)((float) personCounterHistorySum / personCounterHistoryCount);
            if(lastPersonCounter != averageCurrentPersonCounter) {
                //PdBase.sendFloat("number", Math.min(currentPersonCounter, MAX_PD_VALUE));
                PlaySound(averageCurrentPersonCounter);
                lastPersonCounter = averageCurrentPersonCounter;
            }

            setStatus(mTrackerText, String.format("Metric: %d\nAVG: %.1f ( B: %.1f\t, L: %.1f)\nW: %.2f\t (MN: %.0f\tMX: %.0f)", currentPersonCounter, final_sum, base_sum, flow_sum,  weight_base, minValue, maxValue, NumberOfMusicians));
            mLogDetectWriter.appendData(String.format("%s,%d,%f,%f,%f,%f,%f,%f,%d\n", Calendar.getInstance().getTime(), currentPersonCounter, final_sum, base_sum, flow_sum,  weight_base, minValue, maxValue, NumberOfMusicians));
        }

        LastFrame = frameProcessed.clone();
        frame.release();

        // For onCameraFrame, you have to return Mat of the same dimensions as the inputFrame.
        // if flipped it throws an exception if not square
        // https://stackoverflow.com/questions/37970551/opencv-resize-failing
        // https://stackoverflow.com/questions/43547221/change-image-resolution-in-android-with-opencv
        frameCounter++;

        if (frameCounter % FRAME_CALL_GC == 0) {
            // fix opencv memory leak by manual garbage collection
            // https://stackoverflow.com/questions/21050499/memory-leak-from-iterating-opencv-frames
            System.gc();
            System.runFinalization();
            Log.d(TAG, "Garbage Collection");
        }

        return inputFrame.rgba();
    }

    private void PlaySound(int index) {
        PlaySound(index, true);
    }

    private void PlaySound(int index, boolean loop){
        if (index == lastPersonCounter) return;
        StopSound(index);
        for(int i = lastPersonCounter ; i < index; i++) {
            if (mIsSoundPlaying[i] == true) continue;
            mIsSoundPlaying[i] = true;
            mStreamIds[i] = soundPool.play(mSoundIds[i], 0.5f, 0.5f, 0, loop ? -1 : 0, 1);
        }
    }

    private void StopSound(int index){
        for(int i = index ; i < mSoundIds.length; i++) {
            if (mIsSoundPlaying[i] == false) return;
            mIsSoundPlaying[i] = false;
            soundPool.stop(mStreamIds[i]);
        }
    }

    public void onCameraViewStopped() {
    }

    // load file to storage and return a path.
    @NonNull
    private static String getPath(String file, @NonNull Context context) {
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();

            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();

            // Return a path to file
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.e(TAG, "Failed to get file path");
        }
        return "";
    }

    protected static File takeScreenshot(View view, String filename){
        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_hh.mm.ss", date);
        try {
            // Initialising the directory of storage
            String dirpath = Environment.getExternalStorageDirectory() + "";
            File file = new File(dirpath);
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
            }

            // File name
            String path = dirpath + "/ReinheritImages/" + filename + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            //view.setDrawingCacheEnabled(false);
            File imageurl = new File(path);
            FileOutputStream outputStream = new FileOutputStream(imageurl);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            outputStream.flush();
            outputStream.close();
            return imageurl;

        } catch (FileNotFoundException io) {
            io.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bitmap takeSc(View view){
        final Bitmap b = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_HH-mm-ss", date);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request((SurfaceView) view, b, (int result) -> {
                if (result != PixelCopy.SUCCESS) {
                    Toast.makeText(this, "Failed to copy", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    try (FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() +"/ReinheritImages/" + "/surfaceview-"+ format +".png")) {
                        b.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                } catch (Exception e) {
                }
            }, view.getHandler());
        }

        return b;

    }

    public static void saveMatToBitmap(Mat mat, String label){
        Bitmap resultBitmap = null;
        try {
            Mat temp = new Mat(mat.cols(), mat.rows(), CvType.CV_8UC1);
            mat.copyTo(temp);
            Imgproc.cvtColor(temp, temp, Imgproc.COLOR_BGR2RGBA);
            resultBitmap = Bitmap.createBitmap(temp.cols(),temp.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(temp, resultBitmap, true);
            saveBitmap(resultBitmap, label);
        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
        }
    }
    public static Bitmap saveBitmap(Bitmap b){
        return saveBitmap(b, "screenshot_");
    }

    public static Bitmap saveBitmap(Bitmap b, String label){
        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_HH-mm-ss", date);
        try {
            try (FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() +"/ReinheritImages/" + label + format +".png")) {
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        } catch (Exception e) {
        }

        return b;

    }

    private void saveDeviceName(){
        String deviceName; //define a string to keep the name here

        EditText DeviceNameText   = (EditText)findViewById(R.id.editDeviceName); //get the text UI
        deviceName = String.valueOf(DeviceNameText.getText()); //assign the value

        String filePath = Environment.getExternalStorageDirectory().getPath() + "/ReinheritLogs/device_name.txt"; //file path

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
            myWriter.write(deviceName);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
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

    private double calculateFloor(double sum){

        double floorLevel = ((sum - minValue)/(maxValue - minValue)) * 4;
        floorLevel += 1;

        return floorLevel;
    }

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

                //String weight_to_string = myReader.nextLine();


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

        //BT STUFF

    private class ServerClass extends  Thread{

        private BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public ServerClass(){
            try {
                serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket socket = null;

            while (socket == null){
                try {
                    Message message = Message.obtain();
                    message.what = BluetoothService.STATE_CONNECTING;
                    mHandler.sendMessage(message);
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = BluetoothService.STATE_CONNECTION_FAILED;
                    mHandler.sendMessage(message);
                }

                if(socket != null){
                    Message message = Message.obtain();
                    message.what = BluetoothService.STATE_CONNECTED;
                    mHandler.sendMessage(message);

                    //write some code for send / receive

                    break;
                }
            }
        }
    }
}

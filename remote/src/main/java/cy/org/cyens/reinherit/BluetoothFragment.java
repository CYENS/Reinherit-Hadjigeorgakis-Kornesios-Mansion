/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cy.org.cyens.reinherit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.UUID;

import cy.org.cyens.common.BluetoothService;
import cy.org.cyens.common.Constants;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothFragment extends Fragment {

    private static final String TAG = "BluetoothFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    //private ListView mConversationView;
    //private EditText mOutEditText;
    //private Button mSendButton;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    //private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;


    private TextView status;
    /**
     * Member object for the services
     */
    private BluetoothService mService = null;

    public static Hashtable<String, String> DeviceDictionary = new Hashtable<String, String>();//This is a dictionary to save every mac address as a key and the device name as a value

    public static String pathToCSV = Environment.getExternalStorageDirectory() +"/ReinheritLogs/"+"DeviceNamesList.csv";

    private static String currentMacAddress = "MAC not yet defined";

    //BT STUFF
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        String namedev = Settings.Global.DEVICE_NAME;
        CreateFile();
        try {
            ReadCSVFile(pathToCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
/*
        try {
            EditCSVFile("alex", "itsme");
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
/*
        try {
            WriteCSVFile("alex","ff");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            ReadCSVFile(pathToCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
*/

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }


        if (mBluetoothAdapter == null) {
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
        status = activity.findViewById(R.id.textViewStatus);

        @SuppressLint("HardwareIds") String deviceID = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        ((TextView) activity.findViewById(R.id.textViewDeviceID)).setText(deviceID);

        // If BT is not on, request that it be enabled.
        // setup() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the session
        } else if (mService == null) {
            setup();
        }

        /*
        activity.findViewById(R.id.startButton).setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                System.exit(0);
            }
        });
        */

        // TODO For each button write the click listeners to send commands to the connected device(Edit: you can't do that OnCreate. They are initiated in the OnCreatedView)
        /*Setup buttons

        activity.findViewById(R.id.startButton).setOnClickListener(v -> {
            System.out.println("-----------------test test test--------------");
            try {
                sendMessage(new JSONObject().put("id", Constants.COMMANDS.START).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        activity.findViewById(R.id.stopButton).setOnClickListener(v -> {
            try {
                sendMessage(new JSONObject().put("id", Constants.COMMANDS.STOP).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        // Setup frequency slider
        //Slider slider = activity.findViewById(R.id.sliderFreq);
        //slider.addOnChangeListener((slider1, value, fromUser) -> PdBase.sendFloat("freq", value));
        //...
        */

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
//new code more
        Button btn = (Button) view.findViewById(R.id.MusicButton);
        Button btn1 = (Button) view.findViewById(R.id.startButton);
        Button btn2 = (Button) view.findViewById(R.id.stopButton);
        Button btn4 = (Button) view.findViewById(R.id.getStatusButton);
        Button btn5 = (Button) view.findViewById(R.id.cameraButton);
        Button btn6 = (Button) view.findViewById(R.id.raiseVolume);
        Button btn7 = (Button) view.findViewById(R.id.lowerVolume);
        Button btn8 = (Button) view.findViewById(R.id.SetBaseButton);


        Slider slider = (Slider) view.findViewById(R.id.sliderFreq);
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_FREQ).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });                   //PdBase.sendFloat("freq", value));

        Slider slider2 = (Slider) view.findViewById(R.id.sliderMusicians);
        slider2.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_MUSICIANS).put("val",value).toString());
                TextView musiciansText = (TextView) view.findViewById(R.id.sliderMusiciansNumber);
                int convertedValue = (int)value;
                String sliderValue = Integer.toString(convertedValue);
                musiciansText.setText(sliderValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        Slider slider3 = (Slider) view.findViewById(R.id.sliderMaxValue);
        slider3.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_MAX_VALUE).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        Slider slider4 = (Slider) view.findViewById(R.id.sliderMinValue);
        slider4.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_MIN_VALUE).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        Slider slider5 = (Slider) view.findViewById(R.id.sliderWeight);
        slider5.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_WEIGHT).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });



        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// display a message by using a Toast
                Toast.makeText(getActivity(), "Music...", Toast.LENGTH_LONG).show();
                try {
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.PLAY_SOUND).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Starting...", Toast.LENGTH_LONG).show();
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.START).put("val","0").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Stopping...", Toast.LENGTH_LONG).show();
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.STOP).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Getting status...", Toast.LENGTH_LONG).show();
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.GET_STATUS).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Camera...", Toast.LENGTH_LONG).show();
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.CAMERA_DISPLAY).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        btn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// display a message by using a Toast
                //Toast.makeText(getActivity(), "Music...", Toast.LENGTH_LONG).show();
                try {
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.RAISE_VOLUME).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        btn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// display a message by using a Toast
                //Toast.makeText(getActivity(), "Music...", Toast.LENGTH_LONG).show();
                try {
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.LOWER_VOLUME).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        btn8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_BASE_IMAGE).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btnResetLog = (Button) view.findViewById(R.id.buttonResetLog);
        btnResetLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendMessage(new JSONObject().put("id", Constants.COMMANDS.RESET_LOG).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        return view;
        //end of new code more
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //mConversationView = view.findViewById(R.id.in);
        //mOutEditText = view.findViewById(R.id.edit_text_out);
        //mSendButton = view.findViewById(R.id.button_send);
    }

    /**
     * Set up the UI and background operations.
     */
    private void setup() {
        Log.d(TAG, "setup()");

        // Initialize the array adapter for the conversation thread
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        /*
        mConversationArrayAdapter = new ArrayAdapter<>(activity, R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(v -> {
            // Send a message using content of the edit text widget
            View view = getView();
            if (null != view) {
                TextView textView = view.findViewById(R.id.edit_text_out);
                String message = textView.getText().toString();
                sendMessage(message);
            }
        });
        */
        // Initialize the BluetoothService to perform bluetooth connections
        mService = new BluetoothService(activity, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer();
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    @SuppressLint("MissingPermission")
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     *
     private final TextView.OnEditorActionListener mWriteListener
     = (view, actionId, event) -> {
     // If the action is a key-up event on the return key, send the message
     if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
     String message = view.getText().toString();
     sendMessage(message);
     }
     return true;
     };
     */
    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        status.setText(resId);

    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        status.setText(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;//uncomment this
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);//new code
                    char prefix = readMessage.charAt(0);

                    StringBuilder sb = new StringBuilder(readMessage);
                    sb.deleteCharAt(0);
                    readMessage = sb.toString();

                    switch(prefix){

                        case 'n':
                            Toast.makeText(activity,"Device name is: "+readMessage,
                                    Toast.LENGTH_LONG).show();
                            try {
                                EditCSVFile(currentMacAddress,readMessage);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;

                        case 's':
                            Toast.makeText(activity,"Weights: "+readMessage,
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                    /*
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    Bitmap bitmap=BitmapFactory.decodeByteArray(readBuf,0,msg.arg1);
                    try (FileOutputStream out = new FileOutputStream(Environment.getExternalStorageDirectory() +"/ReinheritImages/" + "/surfaceview.png")) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                     */

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a session
                    setup();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, R.string.bt_not_enabled_leaving,
                                Toast.LENGTH_SHORT).show();
                        activity.finish();
                    }
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        currentMacAddress = address;
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth, menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.Disconnect: {
                mService.stop();
                return true;
            }
        }
        return false;
    }

    public static void CreateFile(){
        try {
            File myObj = new File(pathToCSV);
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static void ReadCSVFile(String pathCSV) throws IOException {

        DeviceDictionary.clear(); //clear this so that we know is empty
        BufferedReader csvReader = new BufferedReader(new FileReader(pathCSV));
        String row;
        while ((row = csvReader.readLine()) != null) {

             String[] DeviceNames = row.split(",");//this reads each line in the csv and splits it with commas
             DeviceDictionary.put(DeviceNames[0],DeviceNames[1]); //put the data in the dictionary -> MAC address/Device Name
        }
        csvReader.close();

    }

    public static void WriteCSVFile(String macAddress, String deviceName) throws IOException {

        FileWriter csvWriter = new FileWriter(pathToCSV,true);
        csvWriter.append(macAddress);
        csvWriter.append(",");
        csvWriter.append(deviceName);
        csvWriter.append("\n");

        csvWriter.flush();
        csvWriter.close();
    }

    public static void WriteCSVFile(String macAddress) throws IOException {

        FileWriter csvWriter = new FileWriter(pathToCSV,true);
        csvWriter.append(macAddress);
        csvWriter.append(",");
        csvWriter.append("NoNameFound");
        csvWriter.append("\n");

        csvWriter.flush();
        csvWriter.close();
    }

    public static void EditCSVFile(String macAddress, String editedValue) throws IOException {

        ReadCSVFile(pathToCSV);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            DeviceDictionary.replace(macAddress, editedValue);
        }
        FileWriter csvWriter = new FileWriter(pathToCSV);

        Enumeration<String> e = DeviceDictionary.keys();

        while (e.hasMoreElements()){

            String key = e.nextElement();

            csvWriter.append(key);
            csvWriter.append(",");
            csvWriter.append(DeviceDictionary.get(key));
            csvWriter.append("\n");
        }

        csvWriter.flush();
        csvWriter.close();


    }

    public static void RefreshCSVFile() throws IOException {

        FileWriter csvWriter = new FileWriter(pathToCSV);

        Enumeration<String> e = DeviceDictionary.keys();

        while (e.hasMoreElements()){

            String key = e.nextElement();

            csvWriter.append(key);
            csvWriter.append(",");
            csvWriter.append(DeviceDictionary.get(key));
            csvWriter.append("\n");
        }

        csvWriter.flush();
        csvWriter.close();
    }

    //BT STUFF
    private class ClientClass extends Thread{

        private BluetoothDevice device;
        private BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ClientClass (BluetoothDevice device1){

            device = device1;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){

            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

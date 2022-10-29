package cy.org.cyens.reinherit;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;

import cy.org.cyens.common.Constants;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CalibrationController#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CalibrationController extends Fragment {
    private SharedViewModel mViewModel;
    private TextView mLastStatusView;

    public CalibrationController() {
        // Required empty public constructor
    }

    public static CalibrationController newInstance() {
        CalibrationController fragment = new CalibrationController();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calibration_controller, container, false);

        Slider slider3 = (Slider) view.findViewById(R.id.sliderMaxValue);
        slider3.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_MAX_VALUE).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        Slider slider4 = (Slider) view.findViewById(R.id.sliderMinValue);
        slider4.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_MIN_VALUE).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        Slider slider5 = (Slider) view.findViewById(R.id.sliderWeight);
        slider5.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_WEIGHT).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        Button btn1 = (Button) view.findViewById(R.id.startButton);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Starting...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.START).put("val","0").toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        Button btn2 = (Button) view.findViewById(R.id.stopButton);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Stopping...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.STOP).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        Button btn4 = (Button) view.findViewById(R.id.getStatusButton);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Getting status...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.GET_STATUS).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        Button btn6 = (Button) view.findViewById(R.id.raiseVolume);
        btn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// display a message by using a Toast
                //Toast.makeText(getActivity(), "Music...", Toast.LENGTH_LONG).show();
                try {
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.RAISE_VOLUME).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn7 = (Button) view.findViewById(R.id.lowerVolume);
        btn7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
// display a message by using a Toast
                //Toast.makeText(getActivity(), "Music...", Toast.LENGTH_LONG).show();
                try {
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.LOWER_VOLUME).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn8 = (Button) view.findViewById(R.id.SetBaseButton);
        btn8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_BASE_IMAGE).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn5 = (Button) view.findViewById(R.id.cameraOnButton);
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Camera...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.CAMERA_DISPLAY).put("state",true).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        Button btnCameraOff = (Button) view.findViewById(R.id.cameraOffButton);
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Camera...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.CAMERA_DISPLAY).put("state",false).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLastStatusView = view.findViewById(R.id.text_last_status);
        mViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mViewModel.messagesFromBluetooth.observe(getViewLifecycleOwner(), messages -> {
            while(!messages.isEmpty()) {
                String next_message = messages.remove();
                mLastStatusView.setText(next_message);
            }
        });
    }
}
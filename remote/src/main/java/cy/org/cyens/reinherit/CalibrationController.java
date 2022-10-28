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

//new code more
        Button btn1 = (Button) view.findViewById(R.id.startButton);
        Button btn2 = (Button) view.findViewById(R.id.stopButton);
        Button btn4 = (Button) view.findViewById(R.id.getStatusButton);
        Button btn6 = (Button) view.findViewById(R.id.raiseVolume);
        Button btn7 = (Button) view.findViewById(R.id.lowerVolume);
        Button btn8 = (Button) view.findViewById(R.id.SetBaseButton);

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

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mViewModel.messagesFromBluetooth.observe(getViewLifecycleOwner(), messages -> {

        });
    }
}
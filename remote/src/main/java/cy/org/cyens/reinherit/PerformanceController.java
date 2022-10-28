package cy.org.cyens.reinherit;

import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.slider.Slider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cy.org.cyens.common.Constants;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PerformanceController#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PerformanceController extends Fragment {
    SharedViewModel mViewModel;
    TimePickerDialog mClosingTimePicker;
    EditText mClosingTimeText;

    public PerformanceController() {
        // Required empty public constructor
    }

    public static PerformanceController newInstance() {
        PerformanceController fragment = new PerformanceController();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_performance_controller, container, false);

        Slider slider2 = (Slider) view.findViewById(R.id.sliderMusicians);
        slider2.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_MUSICIANS).put("val",value).toString());
                /*
                TextView musiciansText = (TextView) view.findViewById(R.id.sliderMusiciansText);
                int convertedValue = (int)value;
                String sliderValue = Integer.toString(convertedValue);
                musiciansText.setText(sliderValue);
                */
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        Button btn5 = (Button) view.findViewById(R.id.cameraButton);
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Camera...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.CAMERA_DISPLAY).toString());
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

        mClosingTimeText=(EditText) view.findViewById(R.id.closing_time_text);
        mClosingTimeText.setInputType(InputType.TYPE_NULL);
        mClosingTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar cldr = Calendar.getInstance();
                int hour = cldr.get(Calendar.HOUR_OF_DAY);
                int minutes = cldr.get(Calendar.MINUTE);
                // time picker dialog
                mClosingTimePicker = new TimePickerDialog(getActivity(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker tp, int sHour, int sMinute) {
                                mClosingTimeText.setText(sHour + ":" + sMinute);
                            }
                        }, hour, minutes, true);
                mClosingTimePicker.show();
            }
        });

        mViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mViewModel.messagesFromBluetooth.observe(getViewLifecycleOwner(), messages -> {

        });
    }
}
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
    private TextView mLastStatusView;
    boolean isVisible = false;

    public PerformanceController() {
        // Required empty public constructor
        activate();
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

        //remotely log any number of musicians to a specific station
        Slider slider2 = (Slider) view.findViewById(R.id.sliderMusicians);
        slider2.addOnChangeListener((slider1, value, fromUser) -> {
            try {
                mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_MUSICIANS).put("val",value).toString());
            } catch (JSONException e) {
                e.printStackTrace();
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
                try {
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.LOWER_VOLUME).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

        Button btn3 = (Button) view.findViewById(R.id.buttonResetLog);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    Toast.makeText(getActivity(), "Resetting logs...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.RESET_LOG).toString());
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

        Button btnScheduleClosingTime = (Button) view.findViewById(R.id.setClosingTime);
        btnScheduleClosingTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getActivity(), "Setting closing time...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.SET_CLOSING_TIME).put("time", mClosingTimeText.getText().toString()).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btnCancelClosingTime = (Button) view.findViewById(R.id.cancelClosingTime);
        btnCancelClosingTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Toast.makeText(getActivity(), "Cancelling closing time...", Toast.LENGTH_LONG).show();
                    mViewModel.sendMessage(new JSONObject().put("id", Constants.COMMANDS.CANCEL_CLOSING_TIME).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mLastStatusView = view.findViewById(R.id.text_last_status);
        mViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        mViewModel.messagesFromBluetooth.observe(getViewLifecycleOwner(), messages -> {
            if (!isVisible) return;

            while(!messages.isEmpty()) {
                String next_message = messages.remove();
                mLastStatusView.setText(next_message);
            }
        });
    }

    public void activate() {
        isVisible = true;
    }
    public void deactivate() {
        isVisible = false;
    }
}
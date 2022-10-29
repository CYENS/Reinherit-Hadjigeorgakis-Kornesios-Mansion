package cy.org.cyens.reinherit;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.slider.Slider;

import java.lang.reflect.Array;
import java.util.Arrays;

public class GroundFloorMusiciansTracking extends Fragment {

    private static final int AREAS_ON_FLOOR = 6;

    private MusiciansTrackingViewModel mViewModel;

    private Slider[] mSliders;
    private String[] mAreas;

    public static GroundFloorMusiciansTracking newInstance() {
        return new GroundFloorMusiciansTracking();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ground_floor_musicians_tracking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(MusiciansTrackingViewModel.class);

        retrieveSliders(view);
        retrieveSliderValues();
        addSliderListeners();

        addButtonListeners(view);
    }

    private void retrieveSliders(@NonNull View view) {
        if (mSliders == null || mSliders.length != AREAS_ON_FLOOR) {
            mSliders = new Slider[AREAS_ON_FLOOR];
            mAreas = new String[AREAS_ON_FLOOR];
        }

        mSliders[0] = view.findViewById(R.id.sliderKitchenValue);
        mSliders[1] = view.findViewById(R.id.sliderSculpturesValue);
        mSliders[2] = view.findViewById(R.id.sliderHammamValue);
        mSliders[3] = view.findViewById(R.id.sliderWindsValue);
        mSliders[4] = view.findViewById(R.id.sliderFountainValue);
        mSliders[5] = view.findViewById(R.id.sliderVaultValue);

        for(int i = 0 ; i < AREAS_ON_FLOOR ; i++){
            Slider slider = mSliders[i];
            if (slider == null) continue;
            mAreas[i] = slider.getContentDescription().toString();
        }
    }

    private void retrieveSliderValues() {
        for(int i = 0 ; i < AREAS_ON_FLOOR ; i++){
            Slider slider = mSliders[i];
            if (slider == null) continue;
            int previousSliderValue = mViewModel.getMusiciansInArea(mAreas[i]);
            if (previousSliderValue == -1) continue;
            slider.setValue(previousSliderValue);
        }
    }

    private void addSliderListeners(){
        for (Slider slider : mSliders){
            if (slider == null) continue;
            slider.addOnChangeListener(new Slider.OnChangeListener() {
                @Override
                public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                    saveSliderValue(slider);
                }
            });
        }
    }

    private void addButtonListeners(View view){
        Button btnSaveAll = view.findViewById(R.id.saveAllValues);
        btnSaveAll.setOnClickListener(v -> {
            for (Slider slider : mSliders) {
                if (slider == null) continue;
                saveSliderValue(slider);
            }
        });

        Button btnResetAll = view.findViewById(R.id.resetValues);
        btnResetAll.setOnLongClickListener(view1 -> {

            for(int i = 0 ; i < AREAS_ON_FLOOR ; i++){
                Slider slider = mSliders[i];
                if (slider == null) continue;
                if (mAreas[i].equals("Fountain"))
                    setSliderValue(slider, 10);
                else
                    setSliderValue(slider, 0);
            }
            return false;
        });
    }

    private void setAreaValue(String areaName, int value) {
        for(int i = 0 ; i < AREAS_ON_FLOOR ; i++) {
            if (mAreas[i] != areaName) continue;
            Slider slider = mSliders[i];
            slider.setValue(value);
            break;
        }

        mViewModel.writeAreaData(areaName, value);
    }

    private void setSliderValue(Slider slider, int value) {
        slider.setValue(value);
        mViewModel.writeAreaData(slider.getContentDescription().toString(), value);
    }

    private void saveSliderValue(Slider slider) {
        mViewModel.writeAreaData(slider.getContentDescription().toString(), (int) slider.getValue());
    }
}
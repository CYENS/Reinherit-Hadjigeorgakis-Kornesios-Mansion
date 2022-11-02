package cy.org.cyens.reinherit;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ModesAdapter extends FragmentPagerAdapter {
    Context context;
    int totalTabs;

    PerformanceController mPerformanceControllerFragment;
    CalibrationController mCalibrationControllerFragment;
    GroundFloorMusiciansTracking mGroundFloorMusiciansTracking;
    UpperFloorMusiciansTracking mUpperFloorMusiciansTracking;

   public ModesAdapter(Context c, FragmentManager fm, int totalTabs)   {
        super(fm);
        context = c;
        this.totalTabs = totalTabs;
    }
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                if (mPerformanceControllerFragment == null)
                    mPerformanceControllerFragment = new PerformanceController();

                return mPerformanceControllerFragment;
            case 1:
                if (mCalibrationControllerFragment == null)
                    mCalibrationControllerFragment = new CalibrationController();
                return mCalibrationControllerFragment;
            case 2:
                if (mGroundFloorMusiciansTracking == null)
                    mGroundFloorMusiciansTracking = new GroundFloorMusiciansTracking();
                return mGroundFloorMusiciansTracking;
            case 3:
                if (mUpperFloorMusiciansTracking == null)
                    mUpperFloorMusiciansTracking = new UpperFloorMusiciansTracking();
                return mUpperFloorMusiciansTracking;
            default:
                return null;
        }
    }
    @Override
    public int getCount() {
        return totalTabs;
    }

    public void selectTab(int tabPosition) {
        switch (tabPosition) {
            case 0:
                if (mPerformanceControllerFragment == null) return;
                mPerformanceControllerFragment.activate();
            case 1:
                if (mCalibrationControllerFragment == null) return;
                mCalibrationControllerFragment.activate();
            default:
        }
    }

    public void unselectTab(int tabPosition) {
        if (tabPosition == 0 && mPerformanceControllerFragment != null)
            mPerformanceControllerFragment.deactivate();
        if (tabPosition == 1 && mCalibrationControllerFragment != null)
            mCalibrationControllerFragment.deactivate();
    }
}

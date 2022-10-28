package cy.org.cyens.reinherit;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ModesAdapter extends FragmentPagerAdapter {
    Context context;
    int totalTabs;

   public ModesAdapter(Context c, FragmentManager fm, int totalTabs)   {
        super(fm);
        context = c;
        this.totalTabs = totalTabs;
    }
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                PerformanceController mPerformanceControllerFragment = new PerformanceController();
                return mPerformanceControllerFragment;
            case 1:
                CalibrationController mCalibrationControllerFragment = new CalibrationController();
                return mCalibrationControllerFragment;
            default:
                return null;
        }
    }
    @Override
    public int getCount() {
        return totalTabs;
    }
}

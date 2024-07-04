package app.makorz.cryptofun.ui.settings.infoBox;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PagerAdapter extends FragmentStateAdapter {
    private static final int NUM_PAGES = 4;

    public PagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FirstPageFragment();
            case 1:
                return new SecondPageFragment();
            case 2:
                return new ThirdPageFragment();
            case 3:
                return new FourthPageFragment();
            default:
                return null;
        }
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}



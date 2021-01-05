package kr.jne.hs.jangseongv.rooftop;

import androidx.fragment.app.Fragment;

interface NavigationHost {
    void navigateTo(Fragment fragment, boolean addToBackstack);
}

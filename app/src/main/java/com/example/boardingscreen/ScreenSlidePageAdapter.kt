package com.example.boardingscreen

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

@Suppress("DEPRECATION")
class ScreenSlidePageAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int {
        return 4 // Total number of pages
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> BoardingFragment2()
            1 -> BoardingFragment1()
            2 -> BoardingFragment3()
            3 -> Get_Started()
            else -> throw IndexOutOfBoundsException("Invalid position $position")
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return "Page $position"
    }
}
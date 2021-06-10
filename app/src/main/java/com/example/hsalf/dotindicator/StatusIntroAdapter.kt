package com.example.hsalf.dotindicator

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ScreenSlidePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        val extras = Bundle().apply {
            putInt(PageFragment.EXTRA_PAGE_INDEX, position)
        }
        return PageFragment().apply { arguments = extras }
    }

    companion object {
        const val NUM_PAGES = 3
    }
}
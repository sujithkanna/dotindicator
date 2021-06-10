package com.example.hsalf.dotindicator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        view_pager.adapter = ScreenSlidePagerAdapter(this)
        view_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                dot_indicator.currentPage = position
            }
        })

        dot_indicator.pageTimeoutListener = {
            view_pager.currentItem = view_pager.currentItem.inc() % view_pager.adapter!!.itemCount
        }

        view_pager.currentItem = 0
    }
}
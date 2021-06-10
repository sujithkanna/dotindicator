package com.example.hsalf.dotindicator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        activity_main_pager.adapter = ScreenSlidePagerAdapter(this)
        activity_main_start_dot_indicator.start()
    }
}
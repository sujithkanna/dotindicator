package com.example.hsalf.dotindicator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_page.*

class PageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val page = arguments?.getInt(EXTRA_PAGE_INDEX) ?: 0
        fragment_page_logo.setImageResource(IMAGES[page])
        fragment_page_title.text = "Page ${page.inc()}"
    }

    companion object {
        const val EXTRA_PAGE_INDEX = "extra_page_index"
        val IMAGES = listOf(R.drawable.logo_page1, R.drawable.logo_page2, R.drawable.logo_page3)
    }
}
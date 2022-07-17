package com.thatcakeid.splum

import com.thatcakeid.splum.R
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.thatcakeid.splum.fragments.BrowserFragment


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(intent?.action == Intent.ACTION_VIEW) {
            if (savedInstanceState == null) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainerView, BrowserFragment.newInstance(intent?.data.toString()), "browserFragment")
                    .commit()
            }

        } else {
            if (savedInstanceState == null) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentContainerView, BrowserFragment.newInstance("https://google.com/"), "browserFragment")
                    .commit()
            }
        }
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}

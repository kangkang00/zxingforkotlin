package com.kang.administrator.zhisuntestapplication

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_zxing.*

class ZXingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zxing)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
        }
    }

}

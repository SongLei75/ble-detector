package com.example.wm_detector_client

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
private const val LOG_TAG = "wm_detector_client_debug_info"

class ActivityInfo : AppCompatActivity() {
    private fun blueToothPageUpdate(pageTitle: ActivityHome.PageTitle) {
        when (pageTitle) {
            ActivityHome.PageTitle.HOME->{
                finish()
            }
            ActivityHome.PageTitle.INFO->{
                val titleBar = findViewById<View>(R.id.titleBar_info)
                val titleBarText = titleBar.findViewById<TextView>(R.id.text_mid)
                val titleBarLeftBtn = titleBar.findViewById<Button>(R.id.bt_left)
                titleBarText.text = "软件详情"
                titleBarLeftBtn.text = "返回"
                titleBarLeftBtn.setTextColor(Color.parseColor("#000000"))
                titleBarLeftBtn.setOnClickListener {
                    blueToothPageUpdate(ActivityHome.PageTitle.HOME)
                }
            }
            else -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        Log.d(LOG_TAG, "MainActivity2 onCreate")

        blueToothPageUpdate(ActivityHome.PageTitle.INFO)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "MainActivity2 onDestroy")
    }

    override fun finish() {
        super.finish()
        Log.d(LOG_TAG, "MainActivity2 finish")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "MainActivity2 onResume")
    }
}
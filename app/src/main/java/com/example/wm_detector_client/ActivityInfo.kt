package com.example.wm_detector_client

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.ArrayList

private const val LOG_TAG = "wm_detector_client_debug_info"
private const val VERSION_RELEASE = 0
private const val VERSION_BETA = 1
private const val VERSION_DEBUG = 2

class ActivityInfo : AppCompatActivity() {
    private lateinit var versionInstructionAdapter: MessageAdapter
    private val versionInstructionList = arrayListOf<Version>()

    class Version(val version: String, val type: Int, val context: Context, val detailInfo: Array<String>)
    class MessageAdapter(private val msgList: ArrayList<Version>, context: Context?) :
        BaseAdapter() {
        private val mInflater: LayoutInflater

        override fun getCount(): Int {
            return msgList.size
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItemViewType(position: Int): Int {
            return msgList[position].type
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        @SuppressLint("InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                view = mInflater.inflate(R.layout.activity_info_listview, null)
                viewHolder = ViewHolder()
                viewHolder.version = view.findViewById(R.id.version)
                viewHolder.instruction = view.findViewById(R.id.detailInfo)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            val detailInfoList = mutableListOf<String>()
            for (detailInfoItem in msgList[position].detailInfo) {
                detailInfoList.add(detailInfoItem)
            }
            viewHolder.instruction.adapter = ArrayAdapter(msgList[position].context,
                    R.layout.activity_info_detail_listview, detailInfoList)
            setListViewHeightBasedOnChildren(viewHolder.instruction)

            when (msgList[position].type) {
                VERSION_RELEASE -> {
                    viewHolder.version.text = msgList[position].version
                }
                VERSION_DEBUG -> {
                    viewHolder.version.text = String.format("${msgList[position].version} (DEBUG)")
                }
                VERSION_BETA -> {
                    viewHolder.version.text = String.format("${msgList[position].version} (BETA)")
                }
            }

            return view
        }

        internal class ViewHolder {
            lateinit var version: TextView
            lateinit var instruction: ListView
        }

        init {
            mInflater = LayoutInflater.from(context)
        }
    }

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
                titleBarLeftBtn.text = "〈设备"
                titleBarLeftBtn.setTextColor(Color.parseColor("#000000"))
                titleBarLeftBtn.setOnClickListener {
                    blueToothPageUpdate(ActivityHome.PageTitle.HOME)
                }
            }
            else -> {}
        }
    }

    private fun blueToothVersionInstructionsInit() {
        val releaseNoteLayout = findViewById<ConstraintLayout>(R.id.releaseNote_layout)
        releaseNoteLayout.post {
            val authorTxtView = findViewById<TextView>(R.id.author_info)
            val params = releaseNoteLayout.layoutParams
            params.height = authorTxtView.top - releaseNoteLayout.top
            releaseNoteLayout.layoutParams = params
        }

        val versionInstructionListView = findViewById<ListView>(R.id.versionInstructionsList)
        versionInstructionAdapter = MessageAdapter(versionInstructionList, this)
        versionInstructionListView.adapter = versionInstructionAdapter

        val versionInstructions = arrayListOf(
            Version("v1.02", VERSION_DEBUG, this, arrayOf("home page debug")),
            Version("v1.01", VERSION_RELEASE, this, arrayOf("project init", "home page init", "info page init")),
        )

        for (versionInstruction in versionInstructions) {
            versionInstructionList.add(versionInstruction)
        }

        val versionNumTxtView = findViewById<TextView>(R.id.versionNum_txt)
        versionNumTxtView.text = versionInstructions[0].version
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        Log.d(LOG_TAG, "ActivityInfo onCreate")

        blueToothPageUpdate(ActivityHome.PageTitle.INFO)
        blueToothVersionInstructionsInit()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "ActivityInfo onDestroy")
    }

    override fun finish() {
        super.finish()
        Log.d(LOG_TAG, "ActivityInfo finish")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "ActivityInfo onResume")
    }
}
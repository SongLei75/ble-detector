package com.example.wm_detector_client

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat.getSystemService
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val LOG_TAG = "wm_detector_client_debug_data"

// 根据手机的分辨率从 dp 的单位 转成为 px(像素)
fun dip2px(context: Context, dpValue: Float): Int {
    // 获取当前手机的像素密度（1个dp对应几个px）
    val scale = context.resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt() // 四舍五入取整
}

// 根据手机的分辨率从 px(像素) 的单位 转成为 dp
fun px2dip(context: Context, pxValue: Float): Int {
    // 获取当前手机的像素密度（1个dp对应几个px）
    val scale = context.resources.displayMetrics.density
    return (pxValue / scale + 0.5f).toInt() // 四舍五入取整
}

class ActivityData : AppCompatActivity() {
    companion object {
        const val DATA_SECOND_UPDATE = "timer update per second"

        const val BATTERY_UPDATE_KEY = "battery update flag"
        const val BATTERY_PERCENT_KEY = "battery percent value"
        const val BATTERY_VOLTAGE_KEY = "battery voltage value"
        const val BATTERY_ELECTRICITY_KEY = "battery electricity value"
        const val BATTERY_CHARGING_KEY = "battery charging electricity value"
        const val BATTERY_WORKING_KEY = "battery working electricity value"
        const val BATTERY_TEMPERATURE_NUM_KEY = "battery temperature sensor num value"
        const val BATTERY_TEMPERATURE_KEY = "battery temperature sensor value"
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val secondUpdateFlag = msg.data.getBoolean(DATA_SECOND_UPDATE)
            if (secondUpdateFlag) {
                findViewById<TextView>(R.id.dataUpdateTime).text = getDate()
                batteryChargingWorkingUIUpdate()
            }

            if (msg.data.getBoolean(BATTERY_UPDATE_KEY, false)) {
                val batteryPercent = msg.data.getInt(BATTERY_PERCENT_KEY)
                val batteryVoltage = msg.data.getInt(BATTERY_VOLTAGE_KEY)
                val batteryElectric = msg.data.getInt(BATTERY_ELECTRICITY_KEY)
                val batteryChargingVal = msg.data.getInt(BATTERY_CHARGING_KEY)
                val batteryWorkingVal = msg.data.getInt(BATTERY_WORKING_KEY)
                bluetoothDataBatteryInfoUpdate(batteryPercent, batteryVoltage, batteryElectric,
                    batteryChargingVal, batteryWorkingVal)
            }

            val sensorNum = msg.data.getInt(BATTERY_TEMPERATURE_NUM_KEY, Int.MAX_VALUE)

            if (sensorNum < batteryInfoTemperatureList.size) {
                batteryInfoTemperatureList[sensorNum].temperature = (msg.data.getInt(
                    BATTERY_TEMPERATURE_KEY, 0).toFloat() / 10)
                batteryInfoTemperatureAdapter.notifyDataSetChanged()
            }
        }
    }

    class SecondUpdateTimeTimerTask(private val handler: Handler) : TimerTask() {
        override fun run() {
            val mMessage = Message.obtain()
            val mBundle = Bundle()
            mBundle.putBoolean(DATA_SECOND_UPDATE, true)
            mMessage.data = mBundle
            handler.sendMessage(mMessage)
        }
    }

    class BatteryTemperatureUpdateTimerTask(private val handler: Handler, private val sensorNum: Int) : TimerTask() {
        override fun run() {
            val mMessage = Message.obtain()
            val mBundle = Bundle()
            mBundle.putInt(BATTERY_TEMPERATURE_NUM_KEY, sensorNum)
            mBundle.putInt(BATTERY_TEMPERATURE_KEY, (0..1000).random())
            mMessage.data = mBundle
            handler.sendMessage(mMessage)
        }
    }

    class BatteryPercentUpdateTimerTask(private val handler: Handler, private val demoDeviceShowFlag: Boolean) : TimerTask() {
        override fun run() {
            val batteryPercent: Int = if (demoDeviceShowFlag) {
                (0..100).random()
            } else {
                100
                // TODO: get battery info from bluetooth
            }
            val batteryVoltage: Int = if (demoDeviceShowFlag) {
                (0..500).random()
            } else {
                500
                // TODO: get battery info from bluetooth
            }
            val batteryElectric: Int = if (demoDeviceShowFlag) {
                (0..10000).random()
            } else {
                10000
                // TODO: get battery info from bluetooth
            }
            val batteryChargingVal: Int = if (demoDeviceShowFlag) {
                (0..1000).random()
            } else {
                1000
                // TODO: get battery info from bluetooth
            }
            val batteryWorkingVal: Int = if (demoDeviceShowFlag) {
                (0..1000).random()
            } else {
                1000
                // TODO: get battery info from bluetooth
            }

            val mMessage = Message.obtain()
            val mBundle = Bundle()
            mBundle.putInt(BATTERY_PERCENT_KEY, batteryPercent)
            mBundle.putInt(BATTERY_VOLTAGE_KEY, batteryVoltage)
            mBundle.putInt(BATTERY_ELECTRICITY_KEY, batteryElectric)
            mBundle.putInt(BATTERY_CHARGING_KEY, batteryChargingVal)
            mBundle.putInt(BATTERY_WORKING_KEY, batteryWorkingVal)
            mBundle.putBoolean(BATTERY_UPDATE_KEY, true)
            mMessage.data = mBundle
            handler.sendMessage(mMessage)
        }
    }

    enum class BatteryWarningsLevel{
        CRITICAL,
        ERROR,
        WARNING
    }
    private val batteryWarningsTable = arrayOf(
        Warnings("MOSFET关断", BatteryWarningsLevel.CRITICAL),
        Warnings("MOSFET关关", BatteryWarningsLevel.ERROR),
        Warnings("MOSFET断断", BatteryWarningsLevel.WARNING),
    )
    class Warnings(val description: String, val level: BatteryWarningsLevel)
    class WarningMessageAdapter(private val msgList: ArrayList<Warnings>, context: Context?) :
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

        @SuppressLint("InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                view = mInflater.inflate(R.layout.activity_data_warnings_listview, null)
                viewHolder = ViewHolder()
                viewHolder.image = view.findViewById(R.id.warningLevelImg)
                viewHolder.description = view.findViewById(R.id.warningDescription)
                viewHolder.description.text = msgList[position].description
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            when (msgList[position].level) {
                BatteryWarningsLevel.CRITICAL -> {
                    viewHolder.image.setImageResource(R.drawable.warning_critical)
                }
                BatteryWarningsLevel.ERROR -> {
                    viewHolder.image.setImageResource(R.drawable.warning_error)
                }
                BatteryWarningsLevel.WARNING -> {
                    viewHolder.image.setImageResource(R.drawable.warning_warning)
                }
            }
            return view
        }

        internal class ViewHolder {
            lateinit var image: ImageView
            lateinit var description: TextView
        }

        init {
            mInflater = LayoutInflater.from(context)
        }
    }

    class Temperature(var sensorName: String, var temperature: Float)
    class TemperatureMessageAdapter(private val msgList: ArrayList<Temperature>,
                                    private val context: Context): BaseAdapter() {
        private val mInflater: LayoutInflater = LayoutInflater.from(context)
        override fun getCount(): Int {
            return msgList.size
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("InflateParams", "SetTextI18n")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val viewHolder: ViewHolder

            if (convertView == null) {
                view = mInflater.inflate(R.layout.activity_data_temperature_listview, null)
                viewHolder = ViewHolder()
                viewHolder.name = view.findViewById(R.id.sensorNameTxt)
                viewHolder.nameModify = view.findViewById(R.id.sensorNameModifyBtn)
                viewHolder.temperature = view.findViewById(R.id.sensorTemperatureTxt)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            viewHolder.name.text = msgList[position].sensorName
            viewHolder.nameModify.setImageResource(R.drawable.name_modify)
            viewHolder.nameModify.tag = position
            viewHolder.nameModify.setOnClickListener{
                val popupWindowView = LayoutInflater.from(context).inflate(R.layout.activity_data_popup_window, null)
                val popupWindowEditTxtView = popupWindowView.findViewById<EditText>(R.id.popupWindowEditTxt)
                popupWindowEditTxtView.hint = msgList[position].sensorName
                val popupWindow = PopupWindow(popupWindowView, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, true)
                val popupWindowConfirmBtn = popupWindowView.findViewById<Button>(R.id.popupWindowConfirmBtn)
                popupWindowConfirmBtn.setOnClickListener {
                    msgList[position].sensorName = popupWindowEditTxtView.text.toString()
                    popupWindow.dismiss()
                }
                popupWindow.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                popupWindow.animationStyle = R.style.popupAnim
                popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            }
            viewHolder.temperature.text = "${msgList[position].temperature} ℃"
            return view
        }

        internal class ViewHolder {
            lateinit var name: TextView
            lateinit var nameModify: ImageView
            lateinit var temperature: TextView
        }
    }

    private var secondUpdateTimer = Timer("Second Update time Timer")
    private var batteryInfoUpdateTimer = Timer("Data Update time Timer")
    private var demoDeviceShowFlag = false
    private var batteryChargingFlag = false
    private var batteryChargingUICnt = 0
    private var batteryWorkingFlag = false
    private var batteryWorkingUICnt = 0
    private var batteryInfoWarningsList = arrayListOf<Warnings>()
    private var batteryInfoTemperatureList = arrayListOf<Temperature>()
    private var batteryTemperatureUpdateTimer = arrayListOf<Timer>()
    private var batteryTemperatureUpdateTask = arrayListOf<BatteryTemperatureUpdateTimerTask>()
    private lateinit var secondUpdateTimerTask: SecondUpdateTimeTimerTask
    private lateinit var batteryInfoUpdateTimerTask: BatteryPercentUpdateTimerTask
    private lateinit var batteryInfoWarningsAdapter: WarningMessageAdapter
    private lateinit var batteryInfoTemperatureAdapter: TemperatureMessageAdapter

    @SuppressLint("SimpleDateFormat")
    fun getDate(): String {
        return if (android.os.Build.VERSION.SDK_INT >= 24){
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        }else{
            val tms = Calendar.getInstance()
            tms.get(Calendar.YEAR).toString() + "-" +
                tms.get(Calendar.MONTH).toString() + "-" +
                tms.get(Calendar.DAY_OF_MONTH).toString() + " " +
                String.format("%02d:%02d:%02d", tms.get(Calendar.HOUR_OF_DAY), tms.get(Calendar.MINUTE), tms.get(Calendar.SECOND))
        }
    }

    @SuppressLint("SetTextI18n")
    fun bluetoothDataBatteryInfoUpdate(batteryPercent: Int, batteryVoltage: Int, batteryElectric: Int,
                                       batteryChargingVal: Int, batteryWorkingVal: Int) {
        fun bluetoothDataBatteryPercentUpdate(percent: Int) {
            val batteryPercentView1g = findViewById<ImageView>(R.id.batteryPercent1_g)
            val batteryPercentView1o = findViewById<ImageView>(R.id.batteryPercent1_o)
            val batteryPercentView1r = findViewById<ImageView>(R.id.batteryPercent1_r)
            val batteryPercentView2g = findViewById<ImageView>(R.id.batteryPercent2_g)
            val batteryPercentView2o = findViewById<ImageView>(R.id.batteryPercent2_o)
            val batteryPercentView3 = findViewById<ImageView>(R.id.batteryPercent3)
            val batteryPercentView4 = findViewById<ImageView>(R.id.batteryPercent4)
            val batteryPercentView5 = findViewById<ImageView>(R.id.batteryPercent5)

            findViewById<TextView>(R.id.batteryPercentNum).text = "$percent%"

            if (percent > 80) {
                batteryPercentView1o.visibility = View.GONE
                batteryPercentView1r.visibility = View.GONE
                batteryPercentView2o.visibility = View.GONE
                batteryPercentView1g.visibility = View.VISIBLE
                batteryPercentView2g.visibility = View.VISIBLE
                batteryPercentView3.visibility = View.VISIBLE
                batteryPercentView4.visibility = View.VISIBLE
                batteryPercentView5.visibility = View.VISIBLE
            } else if (percent > 60) {
                batteryPercentView1o.visibility = View.GONE
                batteryPercentView1r.visibility = View.GONE
                batteryPercentView2o.visibility = View.GONE
                batteryPercentView1g.visibility = View.VISIBLE
                batteryPercentView2g.visibility = View.VISIBLE
                batteryPercentView3.visibility = View.VISIBLE
                batteryPercentView4.visibility = View.VISIBLE
                batteryPercentView5.visibility = View.GONE
            } else if (percent > 40) {
                batteryPercentView1o.visibility = View.GONE
                batteryPercentView1r.visibility = View.GONE
                batteryPercentView2o.visibility = View.GONE
                batteryPercentView1g.visibility = View.VISIBLE
                batteryPercentView2g.visibility = View.VISIBLE
                batteryPercentView3.visibility = View.VISIBLE
                batteryPercentView4.visibility = View.GONE
                batteryPercentView5.visibility = View.GONE
            } else if (percent > 20) {
                batteryPercentView1o.visibility = View.VISIBLE
                batteryPercentView1r.visibility = View.GONE
                batteryPercentView2o.visibility = View.VISIBLE
                batteryPercentView1g.visibility = View.GONE
                batteryPercentView2g.visibility = View.GONE
                batteryPercentView3.visibility = View.GONE
                batteryPercentView4.visibility = View.GONE
                batteryPercentView5.visibility = View.GONE
            } else if (percent > 0) {
                batteryPercentView1o.visibility = View.GONE
                batteryPercentView1r.visibility = View.VISIBLE
                batteryPercentView2o.visibility = View.GONE
                batteryPercentView1g.visibility = View.GONE
                batteryPercentView2g.visibility = View.GONE
                batteryPercentView3.visibility = View.GONE
                batteryPercentView4.visibility = View.GONE
                batteryPercentView5.visibility = View.GONE
            } else {
                batteryPercentView1o.visibility = View.GONE
                batteryPercentView1r.visibility = View.GONE
                batteryPercentView2o.visibility = View.GONE
                batteryPercentView1g.visibility = View.GONE
                batteryPercentView2g.visibility = View.GONE
                batteryPercentView3.visibility = View.GONE
                batteryPercentView4.visibility = View.GONE
                batteryPercentView5.visibility = View.GONE
            }
        }

        fun bluetoothDataBatteryVoltageUpdate(voltage: Int) {
            findViewById<TextView>(R.id.batteryVoltage).text = "${voltage.toFloat() / 100} V"
        }

        fun bluetoothDataBatteryElectricityUpdate(electricity: Int) {
            findViewById<TextView>(R.id.batteryElectricity).text = "${electricity.toFloat() / 100} A"
        }

        fun bluetoothDataBatteryPowerUpdate(voltage: Int, electricity: Int) {
            findViewById<TextView>(R.id.batteryPower).text = "功率: " +
                    "%.2f".format((voltage.toFloat() / 100) * (electricity.toFloat() / 100)) + "W"
        }

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        fun bluetoothDataBatteryChargingElectricityValUpdate(batteryChargingVal: Int) {
            val batteryChargingValSwitch = findViewById<Switch>(R.id.batteryChargingElectricitySwitch)

            if (batteryChargingVal > 0) {
                if (!batteryChargingFlag) {
                    batteryChargingFlag = true
                    batteryChargingUICnt = 0
                }
            } else {
                batteryChargingFlag = false
            }

            if (batteryChargingValSwitch.isChecked) {
                findViewById<TextView>(R.id.batteryChargingElectricity).text = "${batteryChargingVal.toFloat() / 100} A"
            }
        }

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        fun bluetoothDataBatteryWorkingElectricityValUpdate(batteryWorkingVal: Int) {
            val batteryWorkingValSwitch = findViewById<Switch>(R.id.batteryWorkingElectricitySwitch)

            if (batteryWorkingVal > 0) {
                if (!batteryWorkingFlag) {
                    batteryWorkingFlag = true
                    batteryWorkingUICnt = 0
                }
            } else {
                batteryWorkingFlag = false
            }

            if (batteryWorkingValSwitch.isChecked) {
                findViewById<TextView>(R.id.batteryWorkingElectricity).text = "${batteryWorkingVal.toFloat() / 100} A"
            }
        }

        bluetoothDataBatteryPercentUpdate(batteryPercent)
        bluetoothDataBatteryVoltageUpdate(batteryVoltage)
        bluetoothDataBatteryElectricityUpdate(batteryElectric)
        bluetoothDataBatteryPowerUpdate(batteryVoltage, batteryElectric)
        bluetoothDataBatteryChargingElectricityValUpdate(batteryChargingVal)
        bluetoothDataBatteryWorkingElectricityValUpdate(batteryWorkingVal)
    }

    fun batteryChargingWorkingUIUpdate() {
        if (batteryChargingFlag) {
            if (batteryChargingUICnt < 5) {
                batteryChargingUICnt += 1
            } else {
                batteryChargingUICnt = 0
            }

            when (batteryChargingUICnt) {
                0 -> {
                    findViewById<ImageView>(R.id.batteryChargingArrow1_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow1_green).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_green).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_green).visibility = View.GONE
                }
                1 -> {
                    findViewById<ImageView>(R.id.batteryChargingArrow1_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow1_green).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_green).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_green).visibility = View.GONE
                }
                2 -> {
                    findViewById<ImageView>(R.id.batteryChargingArrow1_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow1_green).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_green).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_green).visibility = View.GONE
                }
                3 -> {
                    findViewById<ImageView>(R.id.batteryChargingArrow1_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow1_green).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_green).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_green).visibility = View.VISIBLE
                }
                4 -> {
                    findViewById<ImageView>(R.id.batteryChargingArrow1_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow1_green).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_green).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_green).visibility = View.VISIBLE
                }
                5 -> {
                    findViewById<ImageView>(R.id.batteryChargingArrow1_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow1_green).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow2_green).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryChargingArrow3_green).visibility = View.VISIBLE
                }
            }
        } else {
            findViewById<ImageView>(R.id.batteryChargingArrow1_gray).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.batteryChargingArrow2_gray).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.batteryChargingArrow3_gray).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.batteryChargingArrow1_green).visibility = View.GONE
            findViewById<ImageView>(R.id.batteryChargingArrow2_green).visibility = View.GONE
            findViewById<ImageView>(R.id.batteryChargingArrow3_green).visibility = View.GONE
        }

        if (batteryWorkingFlag) {
            if (batteryWorkingUICnt < 5) {
                batteryWorkingUICnt += 1
            } else {
                batteryWorkingUICnt = 0
            }

            when (batteryWorkingUICnt) {
                0 -> {
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_orange).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_orange).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_orange).visibility = View.GONE
                }
                1 -> {
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_orange).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_orange).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_orange).visibility = View.GONE
                }
                2 -> {
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_orange).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_orange).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_orange).visibility = View.GONE
                }
                3 -> {
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_orange).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_orange).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_orange).visibility = View.VISIBLE
                }
                4 -> {
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_orange).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_orange).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_orange).visibility = View.VISIBLE
                }
                5 -> {
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_gray).visibility = View.VISIBLE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_gray).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow1_orange).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow2_orange).visibility = View.GONE
                    findViewById<ImageView>(R.id.batteryWorkingArrow3_orange).visibility = View.VISIBLE
                }
            }
        } else {
            findViewById<ImageView>(R.id.batteryWorkingArrow1_gray).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.batteryWorkingArrow2_gray).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.batteryWorkingArrow3_gray).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.batteryWorkingArrow1_orange).visibility = View.GONE
            findViewById<ImageView>(R.id.batteryWorkingArrow2_orange).visibility = View.GONE
            findViewById<ImageView>(R.id.batteryWorkingArrow3_orange).visibility = View.GONE
        }
    }

    private fun blueToothPageUpdate(pageTitle: ActivityHome.PageTitle) {
        when (pageTitle) {
            ActivityHome.PageTitle.HOME->{
                finish()
            }
            ActivityHome.PageTitle.DATA_SUMMARY->{
                val titleBar = findViewById<View>(R.id.titleBar_data)
                val titleBarText = titleBar.findViewById<TextView>(R.id.text_mid)
                val titleBarLeftBtn = titleBar.findViewById<Button>(R.id.bt_left)
                titleBarText.text = this.intent.getStringExtra("device name")
                demoDeviceShowFlag = this.intent.getBooleanExtra("demo device show flag", false)
                titleBarLeftBtn.text = "设备"
                titleBarLeftBtn.setTextColor(Color.parseColor("#000000"))
                titleBarLeftBtn.setOnClickListener {
                    blueToothPageUpdate(ActivityHome.PageTitle.HOME)
                }
            }
            else -> {}
        }
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun bluetoothDataBatteryInit() {
        @SuppressLint("CutPasteId")
        fun bluetoothDataBatteryDetailInfoInit() {
            @SuppressLint("SetTextI18n")
            fun bluetoothDataBatteryDetailInfoWarningsInit() {
                batteryInfoWarningsAdapter = WarningMessageAdapter(batteryInfoWarningsList, this)
                val batteryDetailInfoWarningListView = findViewById<ListView>(R.id.batteryDetailInfoWarningList)
                batteryDetailInfoWarningListView.adapter = batteryInfoWarningsAdapter

                if (demoDeviceShowFlag) {
                    for (cnt in (0..(0..10).random())) {
                        batteryInfoWarningsList.add(batteryWarningsTable[batteryWarningsTable.indices.random()])
                        ActivityInfo.Utility.setListViewHeightBasedOnChildren(batteryDetailInfoWarningListView)
                    }
                    val batteryDetailInfoWarningCntView = findViewById<TextView>(R.id.batteryDetailInfoWarningCnt)
                    batteryDetailInfoWarningCntView.text = "[${batteryInfoWarningsList.size}]"
                }
            }

            fun bluetoothDataBatteryDetailInfoTemperatureInit() {
                batteryInfoTemperatureAdapter = TemperatureMessageAdapter(batteryInfoTemperatureList, this)
                val batteryDetailInfoTemperatureListView = findViewById<ListView>(R.id.batteryDetailInfoTemperatureList)
                batteryDetailInfoTemperatureListView.adapter = batteryInfoTemperatureAdapter

                if (demoDeviceShowFlag) {
                    for (num in (0..(0..10).random())) {
                        batteryTemperatureUpdateTimer.add(Timer("battery temperature sensor $num update Timer"))
                        batteryTemperatureUpdateTask.add(BatteryTemperatureUpdateTimerTask(mHandler, num))
                        batteryTemperatureUpdateTimer.last().schedule(batteryTemperatureUpdateTask.last(), 0, 1000)
                        batteryInfoTemperatureList.add(Temperature("传感器 $num", 0F))
                        ActivityInfo.Utility.setListViewHeightBasedOnChildren(batteryDetailInfoTemperatureListView)
                    }
                }
            }

            val batteryDetailInfoScrollView = findViewById<ScrollView>(R.id.batteryDetailInfoScroll)
            batteryDetailInfoScrollView.post {
                val batteryControlLayoutView = findViewById<ConstraintLayout>(R.id.batteryControlLayout)
                val batteryDetailInfoScrollViewParams = batteryDetailInfoScrollView.layoutParams
                batteryDetailInfoScrollViewParams.height = batteryControlLayoutView.top - batteryDetailInfoScrollView.top
                batteryDetailInfoScrollView.layoutParams = batteryDetailInfoScrollViewParams

                val params: ConstraintLayout.LayoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                params.topToBottom = R.id.batteryDetailInfoScroll
                params.marginEnd = dip2px(this, 5F)
                params.marginStart = dip2px(this, 5F)
                batteryControlLayoutView.layoutParams = params
            }

            bluetoothDataBatteryDetailInfoWarningsInit()
            bluetoothDataBatteryDetailInfoTemperatureInit()
        }
        secondUpdateTimerTask = SecondUpdateTimeTimerTask(mHandler)
        secondUpdateTimer.schedule(secondUpdateTimerTask, 0, 1000)

        batteryInfoUpdateTimerTask = BatteryPercentUpdateTimerTask(mHandler, demoDeviceShowFlag)
        batteryInfoUpdateTimer.schedule(batteryInfoUpdateTimerTask, 0, 1000)

        val batteryChargingValSwitch = findViewById<Switch>(R.id.batteryChargingElectricitySwitch)
        batteryChargingValSwitch.isChecked = false
        findViewById<TextView>(R.id.batteryChargingElectricity).text = "关闭"
        batteryChargingValSwitch.setOnClickListener {
            if (batteryChargingValSwitch.isChecked) {
                findViewById<TextView>(R.id.batteryChargingElectricity).text = "..."
            } else {
                findViewById<TextView>(R.id.batteryChargingElectricity).text = "关闭"
            }
        }
        val batteryWorkingValSwitch = findViewById<Switch>(R.id.batteryWorkingElectricitySwitch)
        batteryWorkingValSwitch.isChecked = false
        findViewById<TextView>(R.id.batteryWorkingElectricity).text = "关闭"
        batteryWorkingValSwitch.setOnClickListener {
            if (batteryWorkingValSwitch.isChecked) {
                findViewById<TextView>(R.id.batteryWorkingElectricity).text = "..."
            } else {
                findViewById<TextView>(R.id.batteryWorkingElectricity).text = "关闭"
            }
        }
        bluetoothDataBatteryDetailInfoInit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        blueToothPageUpdate(ActivityHome.PageTitle.DATA_SUMMARY)
        bluetoothDataBatteryInit()
    }

    override fun finish() {
        super.finish()

        secondUpdateTimerTask.cancel()
        batteryInfoUpdateTimerTask.cancel()
        for (num in (0 until batteryInfoTemperatureList.size)) {
            batteryTemperatureUpdateTask[num].cancel()
        }
    }
}
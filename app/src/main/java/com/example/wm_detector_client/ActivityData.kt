package com.example.wm_detector_client

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

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
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val secondUpdateFlag = msg.data.getBoolean(DATA_SECOND_UPDATE)
            if (secondUpdateFlag) {
                findViewById<TextView>(R.id.dataUpdateTime).text = getDate()
                batteryChargingWorkingUIUpdate()
            }

            if (msg.data.getBoolean(BATTERY_UPDATE_KEY)) {
                val batteryPercent = msg.data.getInt(BATTERY_PERCENT_KEY)
                val batteryVoltage = msg.data.getInt(BATTERY_VOLTAGE_KEY)
                val batteryElectric = msg.data.getInt(BATTERY_ELECTRICITY_KEY)
                val batteryChargingVal = msg.data.getInt(BATTERY_CHARGING_KEY)
                val batteryWorkingVal = msg.data.getInt(BATTERY_WORKING_KEY)
                bluetoothDataBatteryInfoUpdate(batteryPercent, batteryVoltage, batteryElectric,
                    batteryChargingVal, batteryWorkingVal)
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

    private var secondUpdateTimer = Timer("Second Update time Timer")
    private var batteryInfoUpdateTimer = Timer("Data Update time Timer")
    private var demoDeviceShowFlag = false
    private var batteryChargingFlag = false
    private var batteryChargingUICnt = 0
    private var batteryWorkingFlag = false
    private var batteryWorkingUICnt = 0
    private lateinit var secondUpdateTimerTask: SecondUpdateTimeTimerTask
    private lateinit var batteryInfoUpdateTimerTask: BatteryPercentUpdateTimerTask

    @SuppressLint("SimpleDateFormat")
    fun getDate(): String {
        return if (android.os.Build.VERSION.SDK_INT >= 24){
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        }else{
            val tms = Calendar.getInstance()
            tms.get(Calendar.YEAR).toString() + "-" +
                tms.get(Calendar.MONTH).toString() + "-" +
                tms.get(Calendar.DAY_OF_MONTH).toString() + " " +
                tms.get(Calendar.HOUR_OF_DAY).toString() + ":" +
                tms.get(Calendar.MINUTE).toString() +":" +
                tms.get(Calendar.SECOND).toString()
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

    private fun bluetoothDataBatteryInit() {
        secondUpdateTimerTask = SecondUpdateTimeTimerTask(mHandler)
        secondUpdateTimer.schedule(secondUpdateTimerTask, 0, 1000)

        batteryInfoUpdateTimerTask = BatteryPercentUpdateTimerTask(mHandler, demoDeviceShowFlag)
        batteryInfoUpdateTimer.schedule(batteryInfoUpdateTimerTask, 0, 1000)

        val batteryChargingValSwitch = findViewById<Switch>(R.id.batteryChargingElectricitySwitch)
        batteryChargingValSwitch.isChecked = false
        batteryChargingValSwitch.setOnClickListener {
            if (batteryChargingValSwitch.isChecked) {
                findViewById<TextView>(R.id.batteryChargingElectricity).text = "..."
            } else {
                findViewById<TextView>(R.id.batteryChargingElectricity).text = "关闭"
            }
        }
        val batteryWorkingValSwitch = findViewById<Switch>(R.id.batteryWorkingElectricitySwitch)
        batteryWorkingValSwitch.isChecked = false
        batteryWorkingValSwitch.setOnClickListener {
            if (batteryWorkingValSwitch.isChecked) {
                findViewById<TextView>(R.id.batteryWorkingElectricity).text = "..."
            } else {
                findViewById<TextView>(R.id.batteryWorkingElectricity).text = "关闭"
            }
        }
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
    }
}
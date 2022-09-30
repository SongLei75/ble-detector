package com.example.wm_detector_client

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val LOG_TAG = "wm_detector_client_debug_data"

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
        const val BATTERY_BATTERY_NUM_KEY = "battery battery unit num value"
        const val BATTERY_BATTERY_VOLTAGE_KEY = "battery battery voltage value"
        const val BATTERY_BATTERY_BALANCE_KEY = "battery battery balancing flag"
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            val secondUpdateFlag = msg.data.getBoolean(DATA_SECOND_UPDATE)
            if (secondUpdateFlag) {
                findViewById<TextView>(R.id.dataUpdateTime).text = getDate("detail")
                batteryChargingWorkingUIUpdate()
                batteryVoltageOverviewUpdate()
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

            val batteryUnitNum = msg.data.getInt(BATTERY_BATTERY_NUM_KEY, Int.MAX_VALUE)
            if (batteryUnitNum < batteryInfoBatteryUnitList.size) {
                batteryInfoBatteryUnitList[batteryUnitNum].voltage = (msg.data.getInt(
                    BATTERY_BATTERY_VOLTAGE_KEY, 0).toFloat() / 1000)
                batteryInfoBatteryUnitList[batteryUnitNum].balanceFlag = (msg.data.getBoolean(
                    BATTERY_BATTERY_BALANCE_KEY, false))
                batteryInfoBatteryUnitAdapter.notifyDataSetChanged()
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

    class BatteryBatteryUnitUpdateTimerTask(private val handler: Handler, private val batteryNum: Int) : TimerTask() {
        override fun run() {
            val mMessage = Message.obtain()
            val mBundle = Bundle()
            mBundle.putInt(BATTERY_BATTERY_NUM_KEY, batteryNum)
            mBundle.putInt(BATTERY_BATTERY_VOLTAGE_KEY, (2500..4000).random())
            mBundle.putBoolean(BATTERY_BATTERY_BALANCE_KEY, (0..1).random() == 0)
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
                val popupWindowView = LayoutInflater.from(context).inflate(R.layout.activity_data_battery_rename_popup_window, null)
                val popupWindowEditTxtView = popupWindowView.findViewById<EditText>(R.id.popupWindowEditTxt)
                popupWindowEditTxtView.hint = msgList[position].sensorName
                val popupWindow = PopupWindow(popupWindowView, ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT, true)
                val popupWindowConfirmBtn = popupWindowView.findViewById<Button>(R.id.popupWindowConfirmBtn)
                popupWindowConfirmBtn.setOnClickListener {
                    val newName = popupWindowEditTxtView.text.toString()
                    if (newName != "") {
                        msgList[position].sensorName = newName
                    }
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

    class BatteryUnit(var voltage: Float, var balanceFlag: Boolean)
    class BatteryUnitMessageAdapter(private val msgList: ArrayList<BatteryUnit>,
                                    context: Context): BaseAdapter() {
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
                view = mInflater.inflate(R.layout.activity_data_battery_unit_listview, null)
                viewHolder = ViewHolder()
                viewHolder.num = view.findViewById(R.id.batteryUnitNumTxt)
                viewHolder.batteryVoltageLayout = view.findViewById(R.id.batteryUnitVoltagePercentLayout)
                viewHolder.balancingFlag = view.findViewById(R.id.batteryUnitBalancingFlagTxt)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            viewHolder.num.text = position.toString()
            viewHolder.balancingFlag.setTextColor(if (msgList[position].balanceFlag) Color.BLACK else Color.GRAY)
            viewHolder.batteryVoltageLayout.findViewById<TextView>(R.id.batteryUnitVoltageTxt).text =
                String.format("%.3f V", msgList[position].voltage)

            var batteryUnitVoltageMax = 0F
            for (item in msgList) if (item.voltage > batteryUnitVoltageMax) {
                batteryUnitVoltageMax = item.voltage
            }
            val batteryUnitVoltagePercent = msgList[position].voltage / (batteryUnitVoltageMax * 1.1F)
            val batteryUnitVoltagePercentLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, batteryUnitVoltagePercent)
            viewHolder.batteryVoltageLayout.findViewById<View>(R.id.batteryUnitVoltagePercentView).layoutParams = batteryUnitVoltagePercentLp
            val batteryUnitVoltageBlankPercentLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1 - batteryUnitVoltagePercent)
            viewHolder.batteryVoltageLayout.findViewById<View>(R.id.batteryUnitVoltageBlankPercentView).layoutParams = batteryUnitVoltageBlankPercentLp

            return view
        }

        internal class ViewHolder {
            lateinit var num: TextView
            lateinit var batteryVoltageLayout: ConstraintLayout
            lateinit var balancingFlag: TextView
        }
    }

    class BMSInfoError(var item: String, var description: String)
    class BMSInfoErrorMessageAdapter(private val msgList: ArrayList<BMSInfoError>,
                                    context: Context): BaseAdapter() {
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
                view = mInflater.inflate(R.layout.activity_data_bms_listview, null)
                viewHolder = ViewHolder()
                viewHolder.item = view.findViewById(R.id.BMSInfoErrorCounterTxt)
                viewHolder.description = view.findViewById(R.id.BMSInfoErrorCounterDescriptionTxt)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            viewHolder.item.text = msgList[position].item
            viewHolder.description.text = msgList[position].description

            return view
        }

        internal class ViewHolder {
            lateinit var item: TextView
            lateinit var description: TextView
        }
    }

    private var secondUpdateTimer = Timer("Second Update time Timer")
    private var batteryInfoUpdateTimer = Timer("Data Update time Timer")
    private var demoDeviceShowFlag = false
    private var batteryChargingFlag = false
    private var batteryChargingUICnt = 0
    private var batteryWorkingFlag = false
    private var batteryWorkingUICnt = 0
    private var batteryMaxVoltage = 0F
    private var batteryMinVoltage = Float.MAX_VALUE
    private var batteryMaxChargingElectricity = 0F
    private var batteryMaxWorkingElectricity = 0F
    private var batteryMaxPerformance = 0
    private var batteryInfoWarningsList = arrayListOf<Warnings>()
    private var batteryInfoTemperatureList = arrayListOf<Temperature>()
    private var batteryInfoBatteryUnitList = arrayListOf<BatteryUnit>()
    private var batteryInfoBMSInfoErrorList = arrayListOf<BMSInfoError>()
    private var batteryTemperatureUpdateTimer = arrayListOf<Timer>()
    private var batteryTemperatureUpdateTask = arrayListOf<BatteryTemperatureUpdateTimerTask>()
    private var batteryBatteryUnitVoltageUpdateTimer = arrayListOf<Timer>()
    private var batteryBatteryUnitVoltageUpdateTask = arrayListOf<BatteryBatteryUnitUpdateTimerTask>()
    private lateinit var secondUpdateTimerTask: SecondUpdateTimeTimerTask
    private lateinit var batteryInfoUpdateTimerTask: BatteryPercentUpdateTimerTask
    private lateinit var batteryInfoWarningsAdapter: WarningMessageAdapter
    private lateinit var batteryInfoTemperatureAdapter: TemperatureMessageAdapter
    private lateinit var batteryInfoBatteryUnitAdapter: BatteryUnitMessageAdapter
    private lateinit var batteryInfoBMSInfoErrorAdapter: BMSInfoErrorMessageAdapter

    @SuppressLint("SimpleDateFormat")
    fun getDate(type: String): String {
        var date = ""
        var time = ""

        when (type) {
            "date" -> {
                date = if (android.os.Build.VERSION.SDK_INT >= 24){
                    SimpleDateFormat("yyyy-MM-dd").format(Date())
                }else{
                    val tms = Calendar.getInstance()
                    tms.get(Calendar.YEAR).toString() + "-" +
                        (tms.get(Calendar.MONTH) + 1).toString() + "-" +
                        tms.get(Calendar.DAY_OF_MONTH).toString()
                }
            }
            "time" -> {
                time = if (android.os.Build.VERSION.SDK_INT >= 24){
                    SimpleDateFormat("HH:mm:ss").format(Date())
                }else{
                    val tms = Calendar.getInstance()
                    String.format("%02d:%02d:%02d", tms.get(Calendar.HOUR_OF_DAY), tms.get(Calendar.MINUTE), tms.get(Calendar.SECOND))
                }
            }
            "detail" -> {
                return if (android.os.Build.VERSION.SDK_INT >= 24){
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                }else{
                    val tms = Calendar.getInstance()

                    tms.get(Calendar.YEAR).toString() + "-" +
                        (tms.get(Calendar.MONTH) + 1).toString() + "-" +
                        tms.get(Calendar.DAY_OF_MONTH).toString() + " " +
                        String.format("%02d:%02d:%02d", tms.get(Calendar.HOUR_OF_DAY), tms.get(Calendar.MINUTE), tms.get(Calendar.SECOND))
                }
            }
        }

        return date + time
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

        fun bluetoothDataBatterySnapshotOverviewUpdate(batteryVoltage: Int, batteryElectric: Int,
                                                       batteryChargingVal: Int, batteryWorkingVal: Int) {
            batteryMaxVoltage = if (batteryVoltage.toFloat() / 100 > batteryMaxVoltage)
                batteryVoltage.toFloat() / 100 else batteryMaxVoltage
            batteryMinVoltage = if (batteryVoltage.toFloat() / 100 < batteryMinVoltage)
                batteryVoltage.toFloat() / 100 else batteryMinVoltage
            batteryMaxChargingElectricity = if (batteryChargingVal.toFloat() / 100 > batteryMaxChargingElectricity)
                batteryChargingVal.toFloat() / 100 else batteryMaxChargingElectricity
            batteryMaxWorkingElectricity = if (batteryWorkingVal.toFloat() / 100 > batteryMaxWorkingElectricity)
                batteryWorkingVal.toFloat() / 100 else batteryMaxWorkingElectricity
            batteryMaxPerformance = if ((batteryVoltage * batteryElectric) / 10000 > batteryMaxPerformance)
                (batteryVoltage * batteryElectric) / 10000 else batteryMaxPerformance

            val batteryMaxVoltageView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxVoltageTxt)
            batteryMaxVoltageView.text = "%.2f V".format(batteryMaxVoltage)
            val batteryMinVoltageView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMinVoltageTxt)
            batteryMinVoltageView.text = "%.2f V".format(batteryMinVoltage)
            val batteryMaxChargingElectricityView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxChargingElectricityTxt)
            batteryMaxChargingElectricityView.text = "%.2f A".format(batteryMaxChargingElectricity)
            val batteryMaxWorkingElectricityView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxWorkingElectricityTxt)
            batteryMaxWorkingElectricityView.text = "%.2f A".format(batteryMaxWorkingElectricity)
            val batteryMaxPerformanceView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxPerfTxt)
            batteryMaxPerformanceView.text = "%d W".format(batteryMaxPerformance)
        }

        bluetoothDataBatteryPercentUpdate(batteryPercent)
        bluetoothDataBatteryVoltageUpdate(batteryVoltage)
        bluetoothDataBatteryElectricityUpdate(batteryElectric)
        bluetoothDataBatteryPowerUpdate(batteryVoltage, batteryElectric)
        bluetoothDataBatteryChargingElectricityValUpdate(batteryChargingVal)
        bluetoothDataBatteryWorkingElectricityValUpdate(batteryWorkingVal)
        bluetoothDataBatterySnapshotOverviewUpdate(batteryVoltage, batteryElectric, batteryChargingVal, batteryWorkingVal)
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

    @SuppressLint("SetTextI18n")
    fun batteryVoltageOverviewUpdate() {
        val batteryDetailInfoVoltageAvgTxtView = findViewById<TextView>(R.id.batteryDetailInfoVoltageAvgTxt)
        val batteryDetailInfoVoltageMaxTxtView = findViewById<TextView>(R.id.batteryDetailInfoVoltageMaxTxt)
        val batteryDetailInfoVoltageMinTxtView = findViewById<TextView>(R.id.batteryDetailInfoVoltageMinTxt)

        var voltageSum = 0F
        var voltageMax = 0F
        var voltageMin = Float.MAX_VALUE

        for (ele in batteryInfoBatteryUnitList) {
            voltageSum += ele.voltage
            if (ele.voltage > voltageMax) {
                voltageMax = ele.voltage
            }
            if(ele.voltage < voltageMin) {
                voltageMin = ele.voltage
            }
        }

        batteryDetailInfoVoltageAvgTxtView.text = "△: " +
            "%.3f".format(voltageSum / (batteryInfoBatteryUnitList.size.toFloat())) + ","
        batteryDetailInfoVoltageMinTxtView.text = "min: " +
                "%.3f".format(voltageMin) + ","
        batteryDetailInfoVoltageMaxTxtView.text = "max: " +
                "%.3f".format(voltageMax) + ","
    }

    @SuppressLint("SetTextI18n", "InflateParams")
    fun batteryVoltageOverviewReset(view: View) {
        if (view.id != R.id.batteryDetailInfoSnapShotOverviewResetBtn)
            return

        val popupWindowView = LayoutInflater.from(this).inflate(R.layout.activity_data_battery_reset_popup_window, null)
        val popupWindowLayout = popupWindowView.findViewById<ConstraintLayout>(R.id.popupWindowLayout)
        val popupWindowConfirmBtnView = popupWindowView.findViewById<Button>(R.id.popupWindowConfirmBtn)
        val popupWindowCancelBtnView = popupWindowView.findViewById<Button>(R.id.popupWindowCancelBtn)

        val popupWindow = PopupWindow(popupWindowView, ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, true)

        popupWindowConfirmBtnView.layoutParams.width = popupWindowLayout.layoutParams.width / 2
        popupWindowConfirmBtnView.setOnClickListener {
            batteryMaxVoltage = 0F
            batteryMinVoltage = Float.MAX_VALUE
            batteryMaxChargingElectricity = 0F
            batteryMaxWorkingElectricity = 0F
            batteryMaxPerformance = 0

            val batteryMaxVoltageView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxVoltageTxt)
            batteryMaxVoltageView.text = "..."
            val batteryMinVoltageView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMinVoltageTxt)
            batteryMinVoltageView.text = "..."
            val batteryMaxChargingElectricityView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxChargingElectricityTxt)
            batteryMaxChargingElectricityView.text = "..."
            val batteryMaxWorkingElectricityView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxWorkingElectricityTxt)
            batteryMaxWorkingElectricityView.text = "..."
            val batteryMaxPerformanceView = findViewById<TextView>(R.id.batteryDetailInfoSnapShotOverviewMaxPerfTxt)
            batteryMaxPerformanceView.text = "..."
            popupWindow.dismiss()
        }

        popupWindowCancelBtnView.layoutParams.width = popupWindowLayout.layoutParams.width / 2
        popupWindowCancelBtnView.setOnClickListener {
            popupWindow.dismiss()
        }

        popupWindow.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        popupWindow.animationStyle = R.style.popupAnim
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    fun batteryCloseDischargePort(view: View) {
        if (view.id != R.id.batteryControlTurnOffBtn)
            return

        val popupWindowView = LayoutInflater.from(this).inflate(R.layout.activity_data_battery_control_popup_window, null)
        val popupWindowLayout = popupWindowView.findViewById<ConstraintLayout>(R.id.popupWindowLayout)
        val popupWindowConfirmBtnView = popupWindowView.findViewById<Button>(R.id.popupWindowConfirmBtn)
        val popupWindowCancelBtnView = popupWindowView.findViewById<Button>(R.id.popupWindowCancelBtn)
        val popupWindowTitleTextView = popupWindowView.findViewById<TextView>(R.id.popupWindowContent)
        val popupWindow = PopupWindow(popupWindowView, ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, true)

        popupWindowConfirmBtnView.layoutParams.width = popupWindowLayout.layoutParams.width / 2
        popupWindowConfirmBtnView.setOnClickListener {
            //TODO: close discharge port
            popupWindow.dismiss()
            Toast.makeText(this, "关闭成功", Toast.LENGTH_SHORT).show()
        }

        popupWindowCancelBtnView.layoutParams.width = popupWindowLayout.layoutParams.width / 2
        popupWindowCancelBtnView.setOnClickListener {
            popupWindow.dismiss()
        }

        popupWindowTitleTextView.text = "是否确认关闭放电端口？"
        popupWindow.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        popupWindow.animationStyle = R.style.popupAnim
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    fun batteryOpenDischargePort(view: View) {
        if (view.id != R.id.batteryControlTurnOnBtn)
            return

        val popupWindowView = LayoutInflater.from(this).inflate(R.layout.activity_data_battery_control_popup_window, null)
        val popupWindowLayout = popupWindowView.findViewById<ConstraintLayout>(R.id.popupWindowLayout)
        val popupWindowConfirmBtnView = popupWindowView.findViewById<Button>(R.id.popupWindowConfirmBtn)
        val popupWindowCancelBtnView = popupWindowView.findViewById<Button>(R.id.popupWindowCancelBtn)
        val popupWindowTitleTextView = popupWindowView.findViewById<TextView>(R.id.popupWindowContent)
        val popupWindow = PopupWindow(popupWindowView, ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT, true)

        popupWindowConfirmBtnView.layoutParams.width = popupWindowLayout.layoutParams.width / 2
        popupWindowConfirmBtnView.setOnClickListener {
            //TODO: close discharge port
            popupWindow.dismiss()
            Toast.makeText(this, "打开成功", Toast.LENGTH_SHORT).show()
        }

        popupWindowCancelBtnView.layoutParams.width = popupWindowLayout.layoutParams.width / 2
        popupWindowCancelBtnView.setOnClickListener {
            popupWindow.dismiss()
        }

        popupWindowTitleTextView.text = "是否确认打开放电端口？"
        popupWindow.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        popupWindow.animationStyle = R.style.popupAnim
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
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
                        setListViewHeightBasedOnChildren(batteryDetailInfoWarningListView)
                    }
                    val batteryDetailInfoWarningCntView = findViewById<TextView>(R.id.batteryDetailInfoWarningCnt)
                    batteryDetailInfoWarningCntView.text = "[%d]".format(batteryInfoWarningsList.size)
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
                    }
                    setListViewHeightBasedOnChildren(batteryDetailInfoTemperatureListView)
                }
            }

            fun bluetoothDataBatteryDetailInfoBatteryUnitInit() {
                batteryInfoBatteryUnitAdapter = BatteryUnitMessageAdapter(batteryInfoBatteryUnitList, this)
                val batteryDetailInfoBatteryUnitListView = findViewById<ListView>(R.id.batteryDetailInfoVoltageList)
                batteryDetailInfoBatteryUnitListView.adapter = batteryInfoBatteryUnitAdapter

                if (demoDeviceShowFlag) {
                    for (num in (0..(0..20).random())) {
                        batteryBatteryUnitVoltageUpdateTimer.add(Timer("battery unit voltage $num update Timer"))
                        batteryBatteryUnitVoltageUpdateTask.add(BatteryBatteryUnitUpdateTimerTask(mHandler, num))
                        batteryBatteryUnitVoltageUpdateTimer.last().schedule(batteryBatteryUnitVoltageUpdateTask.last(), 0, 1000)
                        batteryInfoBatteryUnitList.add(BatteryUnit(0F, false))
                    }
                    setListViewHeightBasedOnChildren(batteryDetailInfoBatteryUnitListView)
                }
            }

            fun bluetoothDataBatteryDetailInfoBMSInfoErrorInit() {
                batteryInfoBMSInfoErrorAdapter = BMSInfoErrorMessageAdapter(batteryInfoBMSInfoErrorList, this)
                val batteryDetailInfoBMSInfoListView = findViewById<ListView>(R.id.batteryDetailInfoBMSInfoList)
                batteryDetailInfoBMSInfoListView.adapter = batteryInfoBMSInfoErrorAdapter
                val batteryInfoBMSInfoErrorArray = arrayOf(
                    BMSInfoError("生产商", "WM-Motor"),
                    BMSInfoError("固件版本", "V1.0"),
                    BMSInfoError("设备名称", "Virtual BMS device"),
                    BMSInfoError("生产日期", getDate("date")),
                    BMSInfoError("电池循环次数", "0"),
                    BMSInfoError("充电电流过大", "0"),
                    BMSInfoError("加载时温度过低", "0"),
                    BMSInfoError("加载过程中温度过高", "0"),
                    BMSInfoError("卸货时温度过低", "0"),
                    BMSInfoError("卸货时温度过高", "0"),
                    BMSInfoError("放电电流过大", "0"),
                    BMSInfoError("电池单位欠压", "0"),
                    BMSInfoError("电池单位过压", "0"),
                    BMSInfoError("电池欠压", "0"),
                    BMSInfoError("电池过压", "0"),
                    BMSInfoError("短路", "0"))

                batteryInfoBMSInfoErrorList.addAll(batteryInfoBMSInfoErrorArray)
                setListViewHeightBasedOnChildren(batteryDetailInfoBMSInfoListView)
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
            bluetoothDataBatteryDetailInfoBatteryUnitInit()
            bluetoothDataBatteryDetailInfoBMSInfoErrorInit()
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

        findViewById<Button>(R.id.batteryControlTurnOffBtn).setOnClickListener {
            batteryCloseDischargePort(it)
        }
        findViewById<Button>(R.id.batteryControlTurnOnBtn).setOnClickListener {
            batteryOpenDischargePort(it)
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
        for (num in (0 until batteryInfoTemperatureList.size)) {
            batteryTemperatureUpdateTask[num].cancel()
        }
    }
}
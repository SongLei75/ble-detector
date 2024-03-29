package com.example.wm_detector_client

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.*

// 根据手机的分辨率从 dp 的单位 转成为 px(像素)
fun dip2px(context: Context, dpValue: Float): Int {
    // 获取当前手机的像素密度（1个dp对应几个px）
    val scale = context.resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt() // 四舍五入取整
}

// 根据手机的分辨率从 px(像素) 的单位 转成为 dp
fun px2dip(context: Context, pxValue: Int): Int {
    // 获取当前手机的像素密度（1个dp对应几个px）
    val scale = context.resources.displayMetrics.density
    return (pxValue / scale + 0.5f).toInt() // 四舍五入取整
}

fun setListViewHeightBasedOnChildren(listView: ListView) {
    //获取ListView对应的Adapter
    val listAdapter = listView.adapter
        ?: // pre-condition
        return
    var totalHeight = 0
    var i = 0
    val len = listAdapter.count
    while (i < len) {
        //listAdapter.getCount()返回数据项的数目
        val listItem = listAdapter.getView(i, null, listView)
        listItem.measure(0, 0) //计算子项View 的宽高
        totalHeight += listItem.measuredHeight //统计所有子项的总高度
        i++
    }
    val params = listView.layoutParams
    params.height = totalHeight + listView.dividerHeight * (listAdapter.count - 1)
    //listView.getDividerHeight()获取子项间分隔符占用的高度
    //params.height最后得到整个ListView完整显示需要的高度
    listView.layoutParams = params
}

private const val LOG_TAG = "wm_detector_client_debug_home"
private const val MY_PERMISSION_REQUEST_CODE = 0
private val UUID_SERVER = UUID.fromString("00000001-0000-1000-8000-00805F9B34FB")

//@SuppressLint("MissingPermission")
//class ActivityHomeBLE {
//    enum class BondedState{
//        PAIRED,
//        UNPAIRED,
//    }
//
//    private lateinit var mHandler: Handler
//    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private lateinit var bluetoothGatt: BluetoothGatt
//    private lateinit var bluetoothWRUUID: UUID
//    private lateinit var bluetoothRDUUID: UUID
//
//    fun openBlueTooth() : Boolean{
//        if (!bluetoothAdapter.isEnabled) {
//            bluetoothAdapter.enable()
//        }
//
//        if (bluetoothAdapter.isDiscovering) {
//            bluetoothAdapter.cancelDiscovery()
//        }
//        return true
//    }
//
//    fun closeBlueTooth() {
//        if (bluetoothAdapter.isEnabled) {
//            bluetoothAdapter.disable()
//        }
//    }
//
//    fun registerBluetoothDeviceListClickHandle() {
//        val searchDevices: BroadcastReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                when (intent.action) {
//                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
//                        Log.d(LOG_TAG, "ACTION_STATE_CHANGED")
//                    }
//                    BluetoothDevice.ACTION_FOUND -> {
//                        val device =
//                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//                        if ((device != null) && !device.name.isNullOrEmpty()) {
//                            Log.d(LOG_TAG, "ACTION_FOUND ${device.name} ${device.address}")
//                        }
//                    }
//                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
//                        Toast.makeText(context, "正在扫描", Toast.LENGTH_SHORT).show()
//                    }
//                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                        Toast.makeText(context, "扫描完成，点击列表中的设备来尝试连接", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        }
//
//        fun registerBluetoothDeviceListClickEvent() {
//            val intent = IntentFilter()
//            intent.apply {
//                addAction(BluetoothDevice.ACTION_FOUND)
//                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
//                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
//                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
//            }
//            registerReceiver(searchDevices, intent)
//        }
//    }
//
//    fun getBlueToothDevByName(name: String) {
//
//    }
//
//    fun connectBlueToothDevice(name: String) {
//        class BlueToothCB : BluetoothGattCallback() {
//            /**
//             * Callback triggered as result of [BluetoothGatt.setPreferredPhy], or as a result of
//             * remote device changing the PHY.
//             *
//             * @param gatt GATT client
//             * @param txPhy the transmitter PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
//             * @param rxPhy the receiver PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
//             * @param status Status of the PHY update operation. [BluetoothGatt.GATT_SUCCESS] if the
//             * operation succeeds.
//             */
//            override fun onPhyUpdate(
//                gatt: BluetoothGatt?,
//                txPhy: Int,
//                rxPhy: Int,
//                status: Int,
//            ) {
//                Log.d(LOG_TAG, "onPhyUpdate")
//            }
//
//            /**
//             * Callback triggered as result of [BluetoothGatt.readPhy]
//             *
//             * @param gatt GATT client
//             * @param txPhy the transmitter PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
//             * @param rxPhy the receiver PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
//             * @param status Status of the PHY read operation. [BluetoothGatt.GATT_SUCCESS] if the
//             * operation succeeds.
//             */
//            override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
//                Log.d(LOG_TAG, "onPhyRead")
//            }
//
//            /**
//             * Callback indicating when GATT client has connected/disconnected to/from a remote
//             * GATT server.
//             *
//             * @param gatt GATT client
//             * @param status Status of the connect or disconnect operation. [ ][BluetoothGatt.GATT_SUCCESS] if the operation succeeds.
//             * @param newState Returns the new connection state. Can be one of [ ][BluetoothProfile.STATE_DISCONNECTED] or [BluetoothProfile.STATE_CONNECTED]
//             */
//            override fun onConnectionStateChange(
//                gatt: BluetoothGatt?, status: Int,
//                newState: Int,
//            ) {
//                if (status != BluetoothGatt.GATT_SUCCESS) {
//                    Log.d(LOG_TAG, "ble connection status: $status, state: $newState")
//                    return
//                }
//
//                var retString = "连接失败"
//                val mMessage = Message.obtain()
//                val mBundle = Bundle()
//                when (newState) {
//                    BluetoothProfile.STATE_CONNECTED -> {
//                        retString = "连接成功"
//                        mBundle.putString(ActivityHome.CONN_KEY, "connect")
//
//                        gatt!!.discoverServices()
//                    }
//                    BluetoothProfile.STATE_DISCONNECTED -> {
//                        retString = "已断开"
//                        mBundle.putString(ActivityHome.CONN_KEY, "disconnect")
//
//                        gatt!!.close()
//                    }
//                    else -> {
//                        Log.w(LOG_TAG, "onConnectionStateChange: $status")
//                        mBundle.putString(ActivityHome.CONN_KEY, "connect error")
//                    }
//                }
//                mMessage.data = mBundle
//                mHandler.sendMessage(mMessage)
//                mBundle.putString(ActivityHome.TOAST_KEY, retString)
//                mMessage.data = mBundle
//                mHandler.sendMessage(mMessage)
//                Log.d(LOG_TAG, "设备: ${gatt!!.device.name} $retString")
//            }
//
//            /**
//             * Callback invoked when the list of remote services, characteristics and descriptors
//             * for the remote device have been updated, ie new services have been discovered.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.discoverServices]
//             * @param status [BluetoothGatt.GATT_SUCCESS] if the remote device has been explored
//             * successfully.
//             */
//            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
//                val bluetoothGattServiceList = gatt!!.services
//                Log.d(LOG_TAG, "services num: " + bluetoothGattServiceList.size)
//                for (service in bluetoothGattServiceList) {
//                    Log.d(LOG_TAG, "service： ${service.uuid}")
//                    if (service.uuid == UUID_SERVER) {
//                        val characteristics = service.characteristics
//                        Log.d(LOG_TAG, "characteristics num: " + characteristics.size)
//                        for (characteristic in characteristics) {
//                            Log.d(LOG_TAG, "characteristic: ${characteristic.uuid} ${characteristic.properties} ${characteristic.permissions}")
//                            if ((characteristic.properties and PROPERTY_WRITE) != 0) {
//                                bluetoothWRUUID = characteristic.uuid
//                            }
//                            if ((characteristic.properties and PROPERTY_NOTIFY) != 0) {
//                                val descriptors = characteristic.descriptors
//                                Log.d(LOG_TAG, "descriptors num: " + descriptors.size)
//                                for (descriptor in descriptors) {
//                                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                                    Log.d(LOG_TAG, "write descriptor ret:${gatt.writeDescriptor(descriptor)}")
//                                }
//                                bluetoothRDUUID = characteristic.uuid
//                            }
//                        }
//                    }
//                }
//
//                val service = gatt.getService(UUID_SERVER)
//                val rdChar = service.getCharacteristic(bluetoothRDUUID)
//
//                if (gatt.setCharacteristicNotification(rdChar, true)) {
//                    Log.d(LOG_TAG, "succeed to register read characteristic:${rdChar.uuid}")
//                } else {
//                    Log.d(LOG_TAG, "failed to register read characteristic:${rdChar.uuid}")
//                }
//            }
//
//            /**
//             * Callback reporting the result of a characteristic read operation.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.readCharacteristic]
//             * @param characteristic Characteristic that was read from the associated remote device.
//             * @param status [BluetoothGatt.GATT_SUCCESS] if the read operation was completed
//             * successfully.
//             */
//            override fun onCharacteristicRead(
//                gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?,
//                status: Int,
//            ) {
//                Log.d(LOG_TAG, "onCharacteristicRead")
//            }
//
//            /**
//             * Callback indicating the result of a characteristic write operation.
//             *
//             *
//             * If this callback is invoked while a reliable write transaction is
//             * in progress, the value of the characteristic represents the value
//             * reported by the remote device. An application should compare this
//             * value to the desired value to be written. If the values don't match,
//             * the application must abort the reliable write transaction.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.writeCharacteristic]
//             * @param characteristic Characteristic that was written to the associated remote device.
//             * @param status The result of the write operation [BluetoothGatt.GATT_SUCCESS] if the
//             * operation succeeds.
//             */
//            override fun onCharacteristicWrite(
//                gatt: BluetoothGatt?,
//                characteristic: BluetoothGattCharacteristic?, status: Int,
//            ) {
//                if (characteristic == null) {
//                    return
//                }
//
//                if (status != BluetoothGatt.GATT_SUCCESS) {
//                    Log.w(LOG_TAG, "ble write operation failed, content: \"${
//                        String(characteristic.value)
//                    }\"")
//                }
//            }
//
//            /**
//             * Callback triggered as a result of a remote characteristic notification.
//             *
//             * @param gatt GATT client the characteristic is associated with
//             * @param characteristic Characteristic that has been updated as a result of a remote
//             * notification event.
//             */
//            override fun onCharacteristicChanged(
//                gatt: BluetoothGatt?,
//                characteristic: BluetoothGattCharacteristic?,
//            ) {
//                if (characteristic != null) {
//                    val data = String(characteristic.value)
//                    Log.d(LOG_TAG, "received: $data")
//
//                    // 发送一条携带Bundle对象的消息
//                    val mMessage = Message.obtain()
//                    val mBundle = Bundle()
//                    mBundle.putString(ActivityHome.TOAST_KEY, data)
//                    mMessage.data = mBundle
//                    mHandler.sendMessage(mMessage)
//                }
//            }
//
//            /**
//             * Callback reporting the result of a descriptor read operation.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.readDescriptor]
//             * @param descriptor Descriptor that was read from the associated remote device.
//             * @param status [BluetoothGatt.GATT_SUCCESS] if the read operation was completed
//             * successfully
//             */
//            override fun onDescriptorRead(
//                gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?,
//                status: Int,
//            ) {
//                Log.d(LOG_TAG, "onDescriptorRead")
//            }
//
//            /**
//             * Callback indicating the result of a descriptor write operation.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.writeDescriptor]
//             * @param descriptor Descriptor that was write to the associated remote device.
//             * @param status The result of the write operation [BluetoothGatt.GATT_SUCCESS] if the
//             * operation succeeds.
//             */
//            override fun onDescriptorWrite(
//                gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?,
//                status: Int,
//            ) {
//                Log.d(LOG_TAG, "onDescriptorWrite status:$status")
//            }
//
//            /**
//             * Callback invoked when a reliable write transaction has been completed.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.executeReliableWrite]
//             * @param status [BluetoothGatt.GATT_SUCCESS] if the reliable write transaction was
//             * executed successfully
//             */
//            override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
//                Log.d(LOG_TAG, "onReliableWriteCompleted")
//            }
//
//            /**
//             * Callback reporting the RSSI for a remote device connection.
//             *
//             * This callback is triggered in response to the
//             * [BluetoothGatt.readRemoteRssi] function.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.readRemoteRssi]
//             * @param rssi The RSSI value for the remote device
//             * @param status [BluetoothGatt.GATT_SUCCESS] if the RSSI was read successfully
//             */
//            override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
//                Log.d(LOG_TAG, "onReadRemoteRssi")
//            }
//
//            /**
//             * Callback indicating the MTU for a given device connection has changed.
//             *
//             * This callback is triggered in response to the
//             * [BluetoothGatt.requestMtu] function, or in response to a connection
//             * event.
//             *
//             * @param gatt GATT client invoked [BluetoothGatt.requestMtu]
//             * @param mtu The new MTU size
//             * @param status [BluetoothGatt.GATT_SUCCESS] if the MTU has been changed successfully
//             */
//            override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
//                Log.d(LOG_TAG, "onMtuChanged status:${status} mtu:${mtu}")
//            }
//
//            /**
//             * Callback indicating service changed event is received
//             *
//             *
//             * Receiving this event means that the GATT database is out of sync with
//             * the remote device. [BluetoothGatt.discoverServices] should be
//             * called to re-discover the services.
//             *
//             * @param gatt GATT client involved
//             */
//            override fun onServiceChanged(gatt: BluetoothGatt) {
//                Log.d(LOG_TAG, "onServiceChanged")
//            }
//        }
//
//        class ConnectionSettings constructor(
//            val autoConnect: Boolean = false,
//            allowAutoConnect: Boolean = autoConnect,
//            val transport: Int = BluetoothDevice.TRANSPORT_AUTO,
//            val phy: Int = BluetoothDevice.PHY_LE_1M_MASK,
//        ) {
//            init {
//                if (autoConnect) require(allowAutoConnect)
//            }
//        }
//
//        val connectionSettings = ConnectionSettings()
//        val temp = bluetoothAdapter.getRemoteDevice(device.address)
//        try {
//            bluetoothGatt = with(connectionSettings) {
//                when {
//                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> temp.connectGatt(
//                        this,
//                        autoConnect,
//                        BlueToothCB(),
//                        transport,
//                        phy
//                    )
//                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> temp.connectGatt(this, autoConnect, BlueToothCB(), transport)
//                    else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                        temp.connectGatt(this, autoConnect, BlueToothCB())
//                    } else {
//                        TODO("VERSION.SDK_INT < JELLY_BEAN_MR2")
//                    }
//                }
//            }
//        } catch (exception: IllegalArgumentException) {
//            Log.w(LOG_TAG, "Device not found with provided address.  Unable to connect.")
//        }
//
//        // cancel discovery before connect
//        if (bluetoothAdapter.isDiscovering) {
//            bluetoothAdapter.cancelDiscovery()
//        }
//
//        bluetoothDeviceListClickEventPairing(device)
//        bluetoothDeviceListClickEventConnect(device)
//    }
//}

class ActivityHome : AppCompatActivity() {
    enum class BondedState{
        PAIRED,
        UNPAIRED,
    }

    enum class PageTitle{
        HOME,
        DATA_SUMMARY,
        INFO,
    }

    class SearchDeviceLoadingTimerTask(private val handler: Handler) : TimerTask() {
        private var loadingDotNum = 0
        override fun run() {
            val mMessage = Message.obtain()
            val mBundle = Bundle()
            if (loadingDotNum < 6) {
                loadingDotNum++
            } else {
                loadingDotNum = 0
            }
            mBundle.putInt(LOADING_KEY, loadingDotNum)
            mMessage.data = mBundle
            handler.sendMessage(mMessage)
        }
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var bluetoothWRUUID: UUID
    private lateinit var bluetoothRDUUID: UUID
    private lateinit var searchDeviceLoadingUITimerTask: SearchDeviceLoadingTimerTask
    private var searchDeviceLoadingUITimer = Timer("Device Loading UI Timer")
    private var bluetoothConnState = false
    private val unpairedDevicesList = mutableListOf<BluetoothDevice>()
    private val pairedDevicesList = mutableListOf<BluetoothDevice>()
    private val demoDeviceName = "WM BLE Demo Device"

    // 静态常量
    companion object {
        var ActivityHomeInstance: ActivityHome? = null

        const val TOAST_KEY = "send toast msg"
        const val CONN_KEY = "ble device connection state"
        const val LOADING_KEY = "device searching loading state"
    }

    private fun bluetoothDeviceListView(viewType: String) {
        val bondedBLEDevTextView = findViewById<View>(R.id.bondedBlueToothDeviceListText)
        val bondedBLEDevListView = findViewById<View>(R.id.bondedBlueToothDeviceList)
        val unbondedBLEDevTextView = findViewById<View>(R.id.unbondedBlueToothDeviceListText)
        val unbondedBLEDevListView = findViewById<View>(R.id.unbondedBlueToothDeviceList)
        val demoDeviceView = findViewById<View>(R.id.demoDevice)
        if (viewType == "disconnect") {
            bondedBLEDevTextView.visibility = View.VISIBLE
            bondedBLEDevListView.visibility = View.VISIBLE
            unbondedBLEDevTextView.visibility = View.VISIBLE
            unbondedBLEDevListView.visibility = View.VISIBLE
            demoDeviceView.visibility = View.VISIBLE
        } else if (viewType == "connect") {
            bondedBLEDevTextView.visibility = View.GONE
            bondedBLEDevListView.visibility = View.GONE
            unbondedBLEDevTextView.visibility = View.GONE
            unbondedBLEDevListView.visibility = View.GONE
            demoDeviceView.visibility = View.GONE
        }
    }

    private fun bluetoothDeviceDataView(viewType: String) {
        val dataView = findViewById<View>(R.id.bt_dataView)
        if (viewType == "disconnect") {
            dataView.setBackgroundColor(Color.parseColor("#636363"))
            bluetoothConnState = false
        } else if (viewType == "connect") {
            dataView.setBackgroundColor(Color.parseColor("#3DD239"))
            bluetoothConnState = true
        }
    }

    private fun bluetoothDeviceTestView(viewType: String) {
        val connTestView = findViewById<View>(R.id.bt_connTest)
        if (viewType == "disconnect") {
            connTestView.setBackgroundColor(Color.parseColor("#636363"))
        } else if (viewType == "connect") {
            connTestView.setBackgroundColor(Color.parseColor("#3DD239"))
        }
    }

    fun bluetoothDeviceDisconnectView(viewType: String) {
        val disconnectView = findViewById<View>(R.id.bt_disconnect)
        if (viewType == "disconnect") {
            disconnectView.setBackgroundColor(Color.parseColor("#636363"))
        } else if (viewType == "connect") {
            disconnectView.setBackgroundColor(Color.parseColor("#3DD239"))
        }
    }

    private fun bluetoothDeviceConnStatusRefresh(viewType: String) {
        val connectStatusView = findViewById<View>(R.id.connectStatus)
        val connectFlagView = findViewById<ImageView>(R.id.connectedFlag)

        if (viewType == "disconnect") {
            connectStatusView.visibility = View.GONE
            connectFlagView.visibility = View.GONE
        } else if (viewType == "connect") {
            connectStatusView.visibility = View.VISIBLE
            connectFlagView.visibility = View.VISIBLE
        }
    }

    fun bluetoothDeviceDataSummaryPageUpdate(viewType: String) {
        if (viewType == "disconnect") {
            if (this::bluetoothGatt.isInitialized) {
                Toast.makeText(this@ActivityHome, "连接已断开", Toast.LENGTH_SHORT).show()
            }
        } else if (viewType == "connect") {
            if (this::bluetoothGatt.isInitialized) {
                findViewById<Switch>(R.id.sw_showDemo).isChecked = false
                findViewById<TextView>(R.id.demoDevice).visibility = View.GONE
            }
            blueToothPageUpdate(PageTitle.DATA_SUMMARY)
        }
    }

    // 创建一个Handler
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            var messageContent = msg.data.getString(TOAST_KEY)
            if (messageContent != null) {
                Toast.makeText(this@ActivityHome, messageContent, Toast.LENGTH_SHORT).show()
            }

            messageContent = msg.data.getString(CONN_KEY)
            if (messageContent != null) {
//                bluetoothDeviceListView(messageContent)
//                bluetoothDeviceDataView(messageContent)
//                bluetoothDeviceTestView(messageContent)
//                bluetoothDeviceDisconnectView(messageContent)
//                bluetoothDeviceConnStatusRefresh(messageContent)
                bluetoothDeviceDataSummaryPageUpdate(messageContent)
            }

            val dot1view = findViewById<TextView>(R.id.txt_SearchDeviceDot1)
            val dot2view = findViewById<TextView>(R.id.txt_SearchDeviceDot2)
            val dot3view = findViewById<TextView>(R.id.txt_SearchDeviceDot3)
            val dot4view = findViewById<TextView>(R.id.txt_SearchDeviceDot4)
            val dot5view = findViewById<TextView>(R.id.txt_SearchDeviceDot5)
            val dot6view = findViewById<TextView>(R.id.txt_SearchDeviceDot6)
            when (msg.data.getInt(LOADING_KEY)) {
                6 -> {
                    dot1view.visibility = View.VISIBLE
                    dot2view.visibility = View.VISIBLE
                    dot3view.visibility = View.VISIBLE
                    dot4view.visibility = View.VISIBLE
                    dot5view.visibility = View.VISIBLE
                    dot6view.visibility = View.VISIBLE
                }
                5 -> {
                    dot1view.visibility = View.VISIBLE
                    dot2view.visibility = View.VISIBLE
                    dot3view.visibility = View.VISIBLE
                    dot4view.visibility = View.VISIBLE
                    dot5view.visibility = View.VISIBLE
                    dot6view.visibility = View.GONE
                }
                4 -> {
                    dot1view.visibility = View.VISIBLE
                    dot2view.visibility = View.VISIBLE
                    dot3view.visibility = View.VISIBLE
                    dot4view.visibility = View.VISIBLE
                    dot5view.visibility = View.GONE
                    dot6view.visibility = View.GONE
                }
                3 -> {
                    dot1view.visibility = View.VISIBLE
                    dot2view.visibility = View.VISIBLE
                    dot3view.visibility = View.VISIBLE
                    dot4view.visibility = View.GONE
                    dot5view.visibility = View.GONE
                    dot6view.visibility = View.GONE
                }
                2 -> {
                    dot1view.visibility = View.VISIBLE
                    dot2view.visibility = View.VISIBLE
                    dot3view.visibility = View.GONE
                    dot4view.visibility = View.GONE
                    dot5view.visibility = View.GONE
                    dot6view.visibility = View.GONE
                }
                1 -> {
                    dot1view.visibility = View.VISIBLE
                    dot2view.visibility = View.GONE
                    dot3view.visibility = View.GONE
                    dot4view.visibility = View.GONE
                    dot5view.visibility = View.GONE
                    dot6view.visibility = View.GONE
                }
                0 -> {
                    dot1view.visibility = View.GONE
                    dot2view.visibility = View.GONE
                    dot3view.visibility = View.GONE
                    dot4view.visibility = View.GONE
                    dot5view.visibility = View.GONE
                    dot6view.visibility = View.GONE
                }
            }
        }
    }

    private fun updateListView(listType: BondedState) {
        val bondedList : ListView = findViewById(R.id.bondedBlueToothDeviceList)
        val unbondedList : ListView = findViewById(R.id.unbondedBlueToothDeviceList)
        val deviceNameList = mutableListOf<String>()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (listType == BondedState.PAIRED) {
            for (device in pairedDevicesList) deviceNameList.add(device.name)
            val deviceList = ArrayAdapter(this, R.layout.activity_home_listview,
                deviceNameList)
            bondedList.adapter = deviceList
        } else {
            for (device in unpairedDevicesList) deviceNameList.add(device.name)
            val deviceList = ArrayAdapter(this, R.layout.activity_home_listview,
                deviceNameList)
            unbondedList.adapter = deviceList
        }
    }

    fun addBlueToothDevice(bluetoothDevice: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (bluetoothDevice !in bluetoothAdapter.bondedDevices &&
            bluetoothDevice !in unpairedDevicesList){
            unpairedDevicesList.add(bluetoothDevice)
            updateListView(BondedState.UNPAIRED)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(LOG_TAG, "${permissions[0]} 授权请求被允许")
                } else {
                    if (permissions.isNotEmpty())
                        Log.d(LOG_TAG, "${permissions[0]} 授权请求被拒绝")
                }
                return
            }
        }
    }

    private fun requestBlueToothPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.BLUETOOTH), MY_PERMISSION_REQUEST_CODE)
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.BLUETOOTH_ADMIN), MY_PERMISSION_REQUEST_CODE)
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), MY_PERMISSION_REQUEST_CODE)
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_REQUEST_CODE)

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.BLUETOOTH_PRIVILEGED), MY_PERMISSION_REQUEST_CODE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT), MY_PERMISSION_REQUEST_CODE)
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN), MY_PERMISSION_REQUEST_CODE)
        }
    }

    fun openBlueTooth(view: View) {
        if (view.id != R.id.bt_openBlueTooth) return

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "未授予蓝牙权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            bluetoothAdapter.enable()
        }

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        Toast.makeText(this, "蓝牙已打开", Toast.LENGTH_SHORT).show()
    }

    fun closeBlueTooth(view: View) {
        if (view.id != R.id.bt_closeBlueTooth) return

        fun clearBlueToothDeviceList() {
            val bondedDeviceListView : ListView = findViewById(R.id.bondedBlueToothDeviceList)
            val unbondedDeviceListView : ListView = findViewById(R.id.unbondedBlueToothDeviceList)

            bondedDeviceListView.adapter = null
            unbondedDeviceListView.adapter = null

            pairedDevicesList.clear()
            unpairedDevicesList.clear()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "未授予蓝牙权限", Toast.LENGTH_SHORT).show()
            return
        }

        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
        }

        clearBlueToothDeviceList()
        bluetoothConnState = false
        bluetoothDeviceListView("disconnect")
        bluetoothDeviceDataView("disconnect")
        bluetoothDeviceTestView("disconnect")
        bluetoothDeviceConnStatusRefresh("disconnect")
        Toast.makeText(this, "蓝牙已关闭", Toast.LENGTH_SHORT).show()
    }

    fun listBlueToothDevices(view: View) {
        fun listBlueToothBondedDev() {
            for (bluetoothDevice in bluetoothAdapter.bondedDevices) {
                if (bluetoothDevice !in pairedDevicesList) {
                    pairedDevicesList.add(bluetoothDevice)
                }
            }
            updateListView(BondedState.PAIRED)
        }

        if (view.id != R.id.bt_listDevices) return

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "请先打开蓝牙!", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        listBlueToothBondedDev()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
        searchDeviceLoadingUITimerTask = SearchDeviceLoadingTimerTask(mHandler)
        searchDeviceLoadingUITimer.schedule(searchDeviceLoadingUITimerTask, 0, 500)
    }

    fun testBleConnectionState(view: View) {
        if (view.id != R.id.bt_connTest) return
        if (!bluetoothConnState) return

        val msg = "WM " + SimpleDateFormat.getTimeInstance(2).format(System.currentTimeMillis())

        if (!this::bluetoothGatt.isInitialized) {
            Log.w(LOG_TAG, "gatt service not started")
            Toast.makeText(this, "GATT服务未启动", Toast.LENGTH_SHORT).show()
            return
        }

        val service = bluetoothGatt.getService(UUID_SERVER)
        if (service == null) {
            Log.w(LOG_TAG, "can not find service: $UUID_SERVER")

            for (item in bluetoothGatt.services) {
                "扫描到Service: ${item.uuid}"
            }
            return
        }

        val characteristic = service.getCharacteristic(bluetoothWRUUID)
        if (characteristic == null) {
            Log.w(LOG_TAG, "can not find characteristic: $bluetoothWRUUID")

            for (item in service.characteristics) {
                Log.d(LOG_TAG, "扫描到Characteristic: ${item.uuid}")
            }
            return
        }

        characteristic.setValue(msg)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothGatt.writeCharacteristic(characteristic)
        Log.d(LOG_TAG, "send: $msg")
    }

    fun disconnectBlueToothDevices(view: View) {
        if (view.id != R.id.bt_disconnect) return
        if (!this::bluetoothGatt.isInitialized) {
            Toast.makeText(this, "蓝牙未连接", Toast.LENGTH_SHORT).show()
            return
        }
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!bluetoothConnState) return

        bluetoothGatt.disconnect()
    }

    fun changeDataView(view: View) {
        if (view.id != R.id.bt_dataView) return
        if (!bluetoothConnState) return

        blueToothPageUpdate(PageTitle.DATA_SUMMARY)
    }

    fun blueToothSoftWareInfo(view: View) {
        if (view.id != R.id.img_softwareInfo) return

        blueToothPageUpdate(PageTitle.INFO)
    }

    private fun bluetoothDeviceListClickEventHandle(device: BluetoothDevice)
    {
        fun bluetoothDeviceListClickEventPairing(device: BluetoothDevice) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            Toast.makeText(this, "正在尝试配对设备: ${device.name}", Toast.LENGTH_SHORT).show()
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(this, "${device.name} 已配对过", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "配对" +
                    if (device.createBond()) "成功" else "失败", Toast.LENGTH_SHORT).show()
            }
        }

        fun bluetoothDeviceListClickEventConnect(device: BluetoothDevice) {
            class BlueToothCB : BluetoothGattCallback() {
                /**
                 * Callback triggered as result of [BluetoothGatt.setPreferredPhy], or as a result of
                 * remote device changing the PHY.
                 *
                 * @param gatt GATT client
                 * @param txPhy the transmitter PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
                 * @param rxPhy the receiver PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
                 * @param status Status of the PHY update operation. [BluetoothGatt.GATT_SUCCESS] if the
                 * operation succeeds.
                 */
                override fun onPhyUpdate(
                    gatt: BluetoothGatt?,
                    txPhy: Int,
                    rxPhy: Int,
                    status: Int,
                ) {
                    Log.d(LOG_TAG, "onPhyUpdate")
                }

                /**
                 * Callback triggered as result of [BluetoothGatt.readPhy]
                 *
                 * @param gatt GATT client
                 * @param txPhy the transmitter PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
                 * @param rxPhy the receiver PHY in use. One of [BluetoothDevice.PHY_LE_1M], [ ][BluetoothDevice.PHY_LE_2M], and [BluetoothDevice.PHY_LE_CODED].
                 * @param status Status of the PHY read operation. [BluetoothGatt.GATT_SUCCESS] if the
                 * operation succeeds.
                 */
                override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                    Log.d(LOG_TAG, "onPhyRead")
                }

                /**
                 * Callback indicating when GATT client has connected/disconnected to/from a remote
                 * GATT server.
                 *
                 * @param gatt GATT client
                 * @param status Status of the connect or disconnect operation. [ ][BluetoothGatt.GATT_SUCCESS] if the operation succeeds.
                 * @param newState Returns the new connection state. Can be one of [ ][BluetoothProfile.STATE_DISCONNECTED] or [BluetoothProfile.STATE_CONNECTED]
                 */
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt?, status: Int,
                    newState: Int,
                ) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.d(LOG_TAG, "ble connection status: $status, state: $newState")
                        return
                    }

                    if (ActivityCompat.checkSelfPermission(
                            baseContext,
                            Manifest.permission.BLUETOOTH
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }

                    var retString = "连接失败"
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            retString = "连接成功"

                            val mMessage = Message.obtain()
                            val mBundle = Bundle()
                            mBundle.putString(CONN_KEY, "connect")
                            mMessage.data = mBundle
                            mHandler.sendMessage(mMessage)

                            gatt!!.discoverServices()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            retString = "已断开"

                            val mMessage = Message.obtain()
                            val mBundle = Bundle()
                            mBundle.putString(CONN_KEY, "disconnect")
                            mMessage.data = mBundle
                            mHandler.sendMessage(mMessage)

                            gatt!!.close()
                        }
                        else -> Log.w(LOG_TAG, "onConnectionStateChange: $status")
                    }

                    val mMessage = Message.obtain()
                    val mBundle = Bundle()
                    mBundle.putString(TOAST_KEY, retString)
                    mMessage.data = mBundle
                    mHandler.sendMessage(mMessage)
                    Log.d(LOG_TAG, "设备: ${gatt!!.device.name} $retString")
                }

                /**
                 * Callback invoked when the list of remote services, characteristics and descriptors
                 * for the remote device have been updated, ie new services have been discovered.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.discoverServices]
                 * @param status [BluetoothGatt.GATT_SUCCESS] if the remote device has been explored
                 * successfully.
                 */
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    val bluetoothGattServiceList = gatt!!.services
                    Log.d(LOG_TAG, "services num: " + bluetoothGattServiceList.size)
                    for (service in bluetoothGattServiceList) {
                        Log.d(LOG_TAG, "service： ${service.uuid}")
                        if (service.uuid == UUID_SERVER) {
                            val characteristics = service.characteristics
                            Log.d(LOG_TAG, "characteristics num: " + characteristics.size)
                            for (characteristic in characteristics) {
                                Log.d(LOG_TAG, "characteristic: ${characteristic.uuid} ${characteristic.properties} ${characteristic.permissions}")
                                if ((characteristic.properties and PROPERTY_WRITE) != 0) {
                                    bluetoothWRUUID = characteristic.uuid
                                }
                                if ((characteristic.properties and PROPERTY_NOTIFY) != 0) {
                                    val descriptors = characteristic.descriptors
                                    Log.d(LOG_TAG, "descriptors num: " + descriptors.size)
                                    for (descriptor in descriptors) {
                                        if (ActivityCompat.checkSelfPermission(this@ActivityHome,
                                                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            break
                                        }
                                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                        Log.d(LOG_TAG, "write descriptor ret:${gatt.writeDescriptor(descriptor)}")
                                    }
                                    bluetoothRDUUID = characteristic.uuid
                                }
                            }
                        }
                    }

                    if (ActivityCompat.checkSelfPermission(this@ActivityHome,
                            Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    val service = gatt.getService(UUID_SERVER)
                    val rdChar = service.getCharacteristic(bluetoothRDUUID)

                    if (gatt.setCharacteristicNotification(rdChar, true)) {
                        Log.d(LOG_TAG, "succeed to register read characteristic:${rdChar.uuid}")
                    } else {
                        Log.d(LOG_TAG, "failed to register read characteristic:${rdChar.uuid}")
                    }
                }

                /**
                 * Callback reporting the result of a characteristic read operation.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.readCharacteristic]
                 * @param characteristic Characteristic that was read from the associated remote device.
                 * @param status [BluetoothGatt.GATT_SUCCESS] if the read operation was completed
                 * successfully.
                 */
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?,
                    status: Int,
                ) {
                    Log.d(LOG_TAG, "onCharacteristicRead")
                }

                /**
                 * Callback indicating the result of a characteristic write operation.
                 *
                 *
                 * If this callback is invoked while a reliable write transaction is
                 * in progress, the value of the characteristic represents the value
                 * reported by the remote device. An application should compare this
                 * value to the desired value to be written. If the values don't match,
                 * the application must abort the reliable write transaction.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.writeCharacteristic]
                 * @param characteristic Characteristic that was written to the associated remote device.
                 * @param status The result of the write operation [BluetoothGatt.GATT_SUCCESS] if the
                 * operation succeeds.
                 */
                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?, status: Int,
                ) {
                    if (characteristic == null) {
                        return
                    }

                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.w(LOG_TAG, "ble write operation failed, content: \"${
                            String(characteristic.value)
                        }\"")
                    }
                }

                /**
                 * Callback triggered as a result of a remote characteristic notification.
                 *
                 * @param gatt GATT client the characteristic is associated with
                 * @param characteristic Characteristic that has been updated as a result of a remote
                 * notification event.
                 */
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt?,
                    characteristic: BluetoothGattCharacteristic?,
                ) {
                    if (characteristic != null) {
                        val data = String(characteristic.value)
                        Log.d(LOG_TAG, "received: $data")

                        // 发送一条携带Bundle对象的消息
                        val mMessage = Message.obtain()
                        val mBundle = Bundle()
                        mBundle.putString(TOAST_KEY, data)
                        mMessage.data = mBundle
                        mHandler.sendMessage(mMessage)
                    }
                }

                /**
                 * Callback reporting the result of a descriptor read operation.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.readDescriptor]
                 * @param descriptor Descriptor that was read from the associated remote device.
                 * @param status [BluetoothGatt.GATT_SUCCESS] if the read operation was completed
                 * successfully
                 */
                override fun onDescriptorRead(
                    gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?,
                    status: Int,
                ) {
                    Log.d(LOG_TAG, "onDescriptorRead")
                }

                /**
                 * Callback indicating the result of a descriptor write operation.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.writeDescriptor]
                 * @param descriptor Descriptor that was write to the associated remote device.
                 * @param status The result of the write operation [BluetoothGatt.GATT_SUCCESS] if the
                 * operation succeeds.
                 */
                override fun onDescriptorWrite(
                    gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?,
                    status: Int,
                ) {
                    Log.d(LOG_TAG, "onDescriptorWrite status:$status")
                }

                /**
                 * Callback invoked when a reliable write transaction has been completed.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.executeReliableWrite]
                 * @param status [BluetoothGatt.GATT_SUCCESS] if the reliable write transaction was
                 * executed successfully
                 */
                override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
                    Log.d(LOG_TAG, "onReliableWriteCompleted")
                }

                /**
                 * Callback reporting the RSSI for a remote device connection.
                 *
                 * This callback is triggered in response to the
                 * [BluetoothGatt.readRemoteRssi] function.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.readRemoteRssi]
                 * @param rssi The RSSI value for the remote device
                 * @param status [BluetoothGatt.GATT_SUCCESS] if the RSSI was read successfully
                 */
                override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                    Log.d(LOG_TAG, "onReadRemoteRssi")
                }

                /**
                 * Callback indicating the MTU for a given device connection has changed.
                 *
                 * This callback is triggered in response to the
                 * [BluetoothGatt.requestMtu] function, or in response to a connection
                 * event.
                 *
                 * @param gatt GATT client invoked [BluetoothGatt.requestMtu]
                 * @param mtu The new MTU size
                 * @param status [BluetoothGatt.GATT_SUCCESS] if the MTU has been changed successfully
                 */
                override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                    Log.d(LOG_TAG, "onMtuChanged status:${status} mtu:${mtu}")
                }

                /**
                 * Callback indicating service changed event is received
                 *
                 *
                 * Receiving this event means that the GATT database is out of sync with
                 * the remote device. [BluetoothGatt.discoverServices] should be
                 * called to re-discover the services.
                 *
                 * @param gatt GATT client involved
                 */
                override fun onServiceChanged(gatt: BluetoothGatt) {
                    Log.d(LOG_TAG, "onServiceChanged")
                }
            }

            class ConnectionSettings constructor(
                val autoConnect: Boolean = false,
                allowAutoConnect: Boolean = autoConnect,
                val transport: Int = BluetoothDevice.TRANSPORT_AUTO,
                val phy: Int = BluetoothDevice.PHY_LE_1M_MASK,
            ) {
                init {
                    if (autoConnect) require(allowAutoConnect)
                }
            }

            val connectionSettings = ConnectionSettings()
            val temp = bluetoothAdapter.getRemoteDevice(device.address)
            try {
                bluetoothGatt = with(connectionSettings) {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> temp.connectGatt(
                            baseContext,
                            autoConnect,
                            BlueToothCB(),
                            transport,
                            phy
                        )
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> temp.connectGatt(baseContext, autoConnect, BlueToothCB(), transport)
                        else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            temp.connectGatt(baseContext, autoConnect, BlueToothCB())
                        } else {
                            TODO("VERSION.SDK_INT < JELLY_BEAN_MR2")
                        }
                    }
                }
            } catch (exception: IllegalArgumentException) {
                Log.w(LOG_TAG, "Device not found with provided address.  Unable to connect.")
            }
        }

        // cancel discovery before connect
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothDeviceListClickEventPairing(device)
        bluetoothDeviceListClickEventConnect(device)
    }

    private fun registerBluetoothDeviceListClickHandle() {
        val searchDevices: BroadcastReceiver = object : BroadcastReceiver() {
            @SuppressLint("UseSwitchCompatOrMaterialCode")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        Log.d(LOG_TAG, "ACTION_STATE_CHANGED")
                    }
                    BluetoothDevice.ACTION_FOUND -> {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH)
                            != PackageManager.PERMISSION_GRANTED) {
                            return
                        }
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if ((device != null) && !device.name.isNullOrEmpty()) {
                            addBlueToothDevice(device)
                            Log.d(LOG_TAG, "ACTION_FOUND ${device.name} ${device.address}")
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Toast.makeText(context, "正在扫描", Toast.LENGTH_SHORT).show()
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Toast.makeText(context, "扫描完成，点击列表中的设备来尝试连接", Toast.LENGTH_SHORT).show()
                        searchDeviceLoadingUITimerTask.cancel()
                        val swSearchDevice = findViewById<Switch>(R.id.sw_searchDevice)
                        swSearchDevice.isChecked = false
                        val mMessage = Message.obtain()
                        val mBundle = Bundle()
                        mBundle.putInt(LOADING_KEY, 6)
                        mMessage.data = mBundle
                        mHandler.sendMessage(mMessage)
                    }
                }
            }
        }

        fun registerBluetoothDeviceListClickEvent()
        {
            val intent = IntentFilter()
            intent.apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
            registerReceiver(searchDevices, intent)
        }

        fun registerBluetoothDeviceListClickListen() {
            val bondedDeviceListView : ListView = findViewById(R.id.bondedBlueToothDeviceList)
            val unbondedDeviceListView : ListView = findViewById(R.id.unbondedBlueToothDeviceList)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            bondedDeviceListView.setOnItemClickListener { _, _, position, _ ->
                bluetoothDeviceListClickEventHandle(pairedDevicesList[position])
            }

            unbondedDeviceListView.setOnItemClickListener { _, _, position, _ ->
                bluetoothDeviceListClickEventHandle(unpairedDevicesList[position])
            }
        }

        registerBluetoothDeviceListClickEvent()
        registerBluetoothDeviceListClickListen()
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private fun blueToothHomePageInit() {
        fun blueToothHomePageSearchDeviceInit() {
            val swSearchDevice = findViewById<Switch>(R.id.sw_searchDevice)

            swSearchDevice.setOnClickListener {
                if (swSearchDevice.isChecked) {
                    listBlueToothDevices(findViewById(R.id.bt_listDevices))
                } else {
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (bluetoothAdapter.isDiscovering) {
                            bluetoothAdapter.cancelDiscovery()
                        }
                    }
                }
            }
            swSearchDevice.isChecked = false
        }

        @SuppressLint("SetTextI18n")
        fun blueToothHomePageDemoDeviceInit() {
            fun blueToothHomePageDemoDeviceOnClick() {
                val swShowDemo = findViewById<Switch>(R.id.sw_showDemo)
                val demoDeviceView = findViewById<TextView>(R.id.demoDevice)

                if (swShowDemo.isChecked) {
                    demoDeviceView.visibility = View.VISIBLE
                } else {
                    demoDeviceView.visibility = View.GONE
                }

            }

            val swShowDemo = findViewById<Switch>(R.id.sw_showDemo)
            val demoDeviceView = findViewById<TextView>(R.id.demoDevice)

            swShowDemo.setOnClickListener {
                blueToothHomePageDemoDeviceOnClick()
            }
            swShowDemo.isChecked = true
            demoDeviceView.setOnClickListener {
                val mMessage = Message.obtain()
                val mBundle = Bundle()
                mBundle.putString(CONN_KEY, "connect")
                mMessage.data = mBundle
                mHandler.sendMessage(mMessage)
            }
            demoDeviceView.text = demoDeviceName
            demoDeviceView.visibility = View.VISIBLE
        }

        fun blueToothHomePageDeviceListInit() {
            val viewBlueToothDevices = findViewById<ConstraintLayout>(R.id.blueToothDevices)
            val viewHomePageBottomItem = findViewById<ConstraintLayout>(R.id.homePageBottomItem)
            val listUnbondedDevice = findViewById<ListView>(R.id.unbondedBlueToothDeviceList)

            viewBlueToothDevices.post {
                val params = viewBlueToothDevices.layoutParams
                params.height = viewHomePageBottomItem.top - viewBlueToothDevices.top
                viewBlueToothDevices.layoutParams = params
            }
            listUnbondedDevice.post {

            }
        }

        blueToothHomePageSearchDeviceInit()
        blueToothHomePageDemoDeviceInit()
        blueToothHomePageDeviceListInit()
    }

    private fun blueToothPageUpdate(pageTitle: PageTitle) {
        when (pageTitle) {
            PageTitle.HOME->{
                val titleBar = findViewById<View>(R.id.titleBar_home)
                val titleBarText = titleBar.findViewById<TextView>(R.id.text_mid)
                val titleBarLeftBtn = titleBar.findViewById<Button>(R.id.bt_left)
                titleBarText.text = "设备"
                titleBarLeftBtn.visibility = View.GONE
            }
            PageTitle.DATA_SUMMARY->{
                val intent = Intent()
                intent.setClass(this@ActivityHome, ActivityData::class.java)
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                ) {
                    if (!this::bluetoothGatt.isInitialized) {
                        intent.putExtra("device name", demoDeviceName)
                        intent.putExtra("demo device show flag", true)
                    } else {
                        intent.putExtra("device name", bluetoothGatt.device.name)
                        intent.putExtra("demo device show flag", false)
                    }
                }
                startActivity(intent)
            }
            PageTitle.INFO->{
                val intent = Intent()
                intent.setClass(this@ActivityHome, ActivityInfo::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        ActivityHomeInstance = this
        Log.d(LOG_TAG, "MainActivity onCreate")

        blueToothHomePageInit()
        blueToothPageUpdate(PageTitle.HOME)
        requestBlueToothPermission()
        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        bluetoothAdapter.name = "detector_client"

        registerBluetoothDeviceListClickHandle()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "MainActivity onDestroy")
    }

    override fun finish() {
        super.finish()
        Log.d(LOG_TAG, "MainActivity finish")
    }

    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "MainActivity onResume")

        if (!this::bluetoothGatt.isInitialized) {
            val mMessage = Message.obtain()
            val mBundle = Bundle()
            mBundle.putString(CONN_KEY, "disconnect")
            mMessage.data = mBundle
            mHandler.sendMessage(mMessage)
        }
    }
}
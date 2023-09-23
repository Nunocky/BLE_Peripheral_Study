package com.example.bleperipheralstudy.ui.second

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.delay
import java.util.UUID

// This is a kotlin rewrite of https://qiita.com/poruruba/items/f74c447dd61be26b4ac2

@SuppressWarnings("MissingPermission")
class BLESupport(private val context: Context, private val callback: Callback? = null) {
    interface Callback {
        fun onPrepareStart()
        fun onStartPeripheral()
        fun onStartPeripheralFailed()
        fun onConnected(device: BluetoothDevice)
        fun onDisconnected()
        fun onReadRequest(offset: Int)
        fun onWriteRequest(offset: Int, value: ByteArray)
        fun onNotification()
    }

    private var mBleManager: BluetoothManager? = null
    private var mBleAdapter: BluetoothAdapter? = null
    private var mBtAdvertiser: BluetoothLeAdvertiser? = null
    private var mPsdiCharacteristic: BluetoothGattCharacteristic? = null
    private var mBtCharacteristic1: BluetoothGattCharacteristic? = null
    private var mBtCharacteristic2: BluetoothGattCharacteristic? = null
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var btPsdiService: BluetoothGattService? = null
    private var btGattService: BluetoothGattService? = null
    private var mBtGattServer: BluetoothGattServer? = null
    private var mIsConnected = false
    private var mConnectedDevice: BluetoothDevice? = null

    suspend fun startAdvertising() {
        mBleManager = context.getSystemService(Activity.BLUETOOTH_SERVICE) as BluetoothManager
        mBleAdapter = mBleManager!!.adapter
        if (mBleAdapter != null) {
            prepareBle(context)
        }
    }

    fun stopAdvertising() {
        mBtAdvertiser?.stopAdvertising(object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            }

            override fun onStartFailure(errorCode: Int) {
            }
        })
    }

    /**
     * Preparation of BLE Peripherals
     */
    private suspend fun prepareBle(context: Context) {
        mBtAdvertiser = mBleAdapter!!.bluetoothLeAdvertiser
        if (mBtAdvertiser == null) {
            callback?.onPrepareStart()
            return
        }
        mBtGattServer = mBleManager!!.openGattServer(context, mGattServerCallback)
        btPsdiService = BluetoothGattService(
            UUID_LIFF_PSDI_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        mPsdiCharacteristic = BluetoothGattCharacteristic(
            UUID_LIFF_PSDI,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        btPsdiService!!.addCharacteristic(mPsdiCharacteristic)
        mBtGattServer?.addService(btPsdiService)
        delay(200)

        btGattService = BluetoothGattService(
            UUID_LIFF_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        mBtCharacteristic1 = BluetoothGattCharacteristic(
            UUID_LIFF_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        btGattService!!.addCharacteristic(mBtCharacteristic1)
        mBtCharacteristic2 = BluetoothGattCharacteristic(
            UUID_LIFF_READ,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        btGattService!!.addCharacteristic(mBtCharacteristic2)
        mNotifyCharacteristic = BluetoothGattCharacteristic(
            UUID_LIFF_NOTIFY,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        btGattService!!.addCharacteristic(mNotifyCharacteristic)
        val dataDescriptor = BluetoothGattDescriptor(
            UUID_LIFF_DESC,
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ
        )
        mNotifyCharacteristic!!.addDescriptor(dataDescriptor)
        mBtGattServer?.addService(btGattService)
        delay(200)
        startBleAdvertising()
    }

    /**
     * アドバタイズ開始
     */
    private fun startBleAdvertising() {
        val settings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            setTimeout(0)
            setConnectable(true)
        }.build()

        val data = AdvertiseData.Builder().apply {
            setIncludeTxPowerLevel(true)
            addServiceUuid(ParcelUuid.fromString(UUID_LIFF_SERVICE_STR))
        }.build()

        val resp = AdvertiseData.Builder().apply {
            setIncludeDeviceName(true)  // If this value is set to false, N/A is displayed in the scan result.
        }.build()

        mBtAdvertiser!!.startAdvertising(
            settings,
            data,
            resp,
            object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.d(TAG, "onStartSuccess")
                    callback?.onStartPeripheral()
                }

                override fun onStartFailure(errorCode: Int) {
                    Log.d(TAG, "onStartFailure")
                    callback?.onStartPeripheralFailed()
                }
            })
    }

    private val mGattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            private val psdiValue = ByteArray(8)
            private val notifyDescValue = ByteArray(2)
            private val charValue =
                ByteArray(UUID_LIFF_VALUE_SIZE) /* max 512 */

            override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
                Log.d(TAG, "onMtuChanged($mtu)")
            }

            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                Log.d(TAG, "onConnectionStateChange")
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mConnectedDevice = device
                    mIsConnected = true
                    Log.d(TAG, "STATE_CONNECTED : $device")
                    callback?.onConnected(device)
                } else {
                    mIsConnected = false
                    Log.d(TAG, "Unknown STATE:$newState")
                    callback?.onDisconnected()
                }
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                Log.d(TAG, "onCharacteristicReadRequest")
                if (characteristic.uuid.compareTo(UUID_LIFF_PSDI) == 0) {
                    mBtGattServer!!.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, offset, psdiValue
                    )
                } else if (characteristic.uuid.compareTo(UUID_LIFF_READ) == 0) {
                    callback?.onReadRequest(offset)

                    if (offset > charValue.size) {
                        mBtGattServer!!.sendResponse(
                            device, requestId, BluetoothGatt.GATT_FAILURE, offset, null
                        )
                    } else {
                        val value = ByteArray(charValue.size - offset)
                        System.arraycopy(charValue, offset, value, 0, value.size)
                        mBtGattServer!!.sendResponse(
                            device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value
                        )
                    }
                } else {
                    mBtGattServer!!.sendResponse(
                        device, requestId, BluetoothGatt.GATT_FAILURE, offset, null
                    )
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                Log.d(TAG, "onCharacteristicWriteRequest")
                if (characteristic.uuid.compareTo(UUID_LIFF_WRITE) == 0) {
                    callback?.onWriteRequest(offset, value)

                    if (offset < charValue.size) {
                        var len = value.size
                        if (offset + len > charValue.size) len = charValue.size - offset
                        System.arraycopy(value, 0, charValue, offset, len)
                        mBtGattServer!!.sendResponse(
                            device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null
                        )
                    } else {
                        mBtGattServer!!.sendResponse(
                            device, requestId, BluetoothGatt.GATT_FAILURE, offset, null
                        )
                    }
                    if (notifyDescValue[0].toInt() and 0x01 != 0x00) {
                        if (offset == 0 && value[0] == 0xff.toByte()) {
                            mNotifyCharacteristic!!.value = charValue
                            mBtGattServer!!.notifyCharacteristicChanged(
                                mConnectedDevice,
                                mNotifyCharacteristic,
                                false
                            )
                            callback?.onNotification()
                        }
                    }
                } else {
                    mBtGattServer!!.sendResponse(
                        device, requestId, BluetoothGatt.GATT_FAILURE, offset, null
                    )
                }
            }

            override fun onDescriptorReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor
            ) {
                Log.d(TAG, "onDescriptorReadRequest")
                if (descriptor.uuid.compareTo(UUID_LIFF_DESC) == 0) {
                    mBtGattServer!!.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, offset, notifyDescValue
                    )
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                Log.d(TAG, "onDescriptorWriteRequest")
                if (descriptor.uuid.compareTo(UUID_LIFF_DESC) == 0) {
                    notifyDescValue[0] = value[0]
                    notifyDescValue[1] = value[1]
                    mBtGattServer!!.sendResponse(
                        device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null
                    )
                }
            }
        }

    companion object {
        private const val TAG = "BLEPeripheralStudy"

        private val UUID_LIFF_PSDI_SERVICE = UUID.fromString("e625601e-9e55-4597-a598-76018a0d293d")
        private val UUID_LIFF_PSDI = UUID.fromString("26e2b12b-85f0-4f3f-9fdd-91d114270e6e")

        private const val UUID_LIFF_SERVICE_STR = "a9d158bb-9007-4fe3-b5d2-d3696a3eb067"

        private val UUID_LIFF_SERVICE = UUID.fromString(UUID_LIFF_SERVICE_STR)

        private val UUID_LIFF_WRITE = UUID.fromString("52dc2801-7e98-4fc2-908a-66161b5959b0")
        private val UUID_LIFF_READ = UUID.fromString("52dc2802-7e98-4fc2-908a-66161b5959b0")
        private val UUID_LIFF_NOTIFY = UUID.fromString("52dc2803-7e98-4fc2-908a-66161b5959b0")
        private val UUID_LIFF_DESC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val UUID_LIFF_VALUE_SIZE = 500
    }
}
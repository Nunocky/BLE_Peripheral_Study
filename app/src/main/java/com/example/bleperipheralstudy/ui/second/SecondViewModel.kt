package com.example.bleperipheralstudy.ui.second

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.bleperipheralstudy.util.toHexString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecondViewModel @Inject constructor(
    private val application: Application,
) :
    ViewModel() {

    private val bleSupport by lazy { BLESupport(application, bleSupportCallback) }

    private val _textMessage = MutableStateFlow("")
    private val _textAddress = MutableStateFlow("")
    private val _textAccess = MutableStateFlow("")
    private val _textOffset = MutableStateFlow("")
    private val _textLength = MutableStateFlow("")
    private val _textValue = MutableStateFlow("")

    val textMessage = _textMessage.asLiveData()
    val textAddress = _textAddress.asLiveData()
    val textAccess = _textAccess.asLiveData()
    val textOffset = _textOffset.asLiveData()
    val textLength = _textLength.asLiveData()
    val textValue = _textValue.asLiveData()

    fun startAdvertising() {
        viewModelScope.launch {
            bleSupport.startAdvertising()
        }
    }

    fun stopAdvertising() {
        viewModelScope.launch {
            bleSupport.stopAdvertising()
        }
    }

    private val bleSupportCallback = object : BLESupport.Callback {
        override fun onPrepareStart() {
            Toast.makeText(application, "BLE Peripheralモードが使用できません。", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onStartPeripheral() {
            _textMessage.value = "ペリフェラルを開始しました"
        }

        override fun onStartPeripheralFailed() {
            _textMessage.value = "ペリフェラルを開始できませんでした"
        }

        override fun onConnected(device: BluetoothDevice) {
            _textMessage.value = "接続されました"
            _textAddress.value = device.address
        }

        override fun onDisconnected() {
            _textMessage.value = "切断されました"
            _textAddress.value = ""
        }

        override fun onReadRequest(offset: Int) {
            _textAccess.value = "Read"
            _textOffset.value = offset.toString()
            _textLength.value = ""
            _textValue.value = ""
        }

        override fun onWriteRequest(offset: Int, value: ByteArray) {
            _textAccess.value = "Write"
            _textOffset.value = offset.toString()
            _textLength.value = value.size.toString()
            _textValue.value = toHexString(value)
        }

        override fun onNotification() {
            _textMessage.value = "Notificationしました"
        }
    }
}
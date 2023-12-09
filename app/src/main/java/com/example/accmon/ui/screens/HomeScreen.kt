package com.example.accmon.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.accmon.R
import com.example.accmon.data.ConnectedDevice
import com.example.accmon.ui.theme.AndroidGreen
import com.example.accmon.ui.theme.BluetoothBlue
import com.example.accmon.ui.theme.StyleBlue
import com.example.accmon.ui.theme.ThemeBlack
import com.example.accmon.ui.theme.ThemeBlue
import com.example.accmon.ui.viewmodels.MoveSenseVM
import com.polar.sdk.api.model.PolarSensorSetting
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: MoveSenseVM,
    navController: NavHostController
){
    val recordWithBlueToothDevice by vm.recordWithBluetoothDevice.collectAsState()
    val isRecording by vm.isRecording.collectAsState()
    val accelerometer = vm.getAccelerometer()
    val bluetoothDeviceScanner = vm.getBluetoothScanner()
    val discoveredDevices by vm.foundBluetoothDevices.collectAsState()
    val isBluetoothSearching by vm.isBluetoothSearching.collectAsState()
    val connectedDevice by vm.connectedDevice.collectAsState()

    val numOfDiscoveredDevices by vm.numOfDiscoveredDevices.collectAsState()
    var accelerometerValues by remember { mutableStateOf("No data") }
    var isConnected by remember { mutableStateOf(false) }
    var isStreamingAccData by remember { mutableStateOf(false) }

    // Observe changes in accelerometer values
    DisposableEffect(accelerometer) {
        val callback: (Float, Float, Float) -> Unit = { x, y, z ->
            // Update the UI with the new accelerometer values
            accelerometerValues = "X: ${String.format("%.3f", x).toFloat()}\nY: ${String.format("%.3f", y).toFloat()}\nZ: ${String.format("%.3f", z).toFloat()}"
        }

        // Set the callback to be notified when accelerometer values change
        accelerometer.setOnSensorChangedCallback(callback)

        // Remove the callback when the composable is disposed
        onDispose {
            accelerometer.setOnSensorChangedCallback { _, _, _ -> /* do nothing */ }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            // Check for internet connectivity

            isConnected = ((connectedDevice.bluetoothVariant != null) && bluetoothDeviceScanner.isConnected())

            // Check internet interval
            delay(200)
        }
    }

    val settings: MutableMap<PolarSensorSetting.SettingType, Int> = mutableMapOf()
    settings[PolarSensorSetting.SettingType.SAMPLE_RATE] = 52
    settings[PolarSensorSetting.SettingType.RESOLUTION] = 16
    settings[PolarSensorSetting.SettingType.RANGE] = 8
    settings[PolarSensorSetting.SettingType.CHANNELS] = 3


    // Main Column for HomeScreen
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBlue)
            .padding(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Column (
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text(
                text = "MoveSense",
                fontSize = 70.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive,
                color = Color.White
            )
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Row {
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(
                        text = "Bluetooth Devices",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                    ){
                        if (isBluetoothSearching) {
                            CircularProgressIndicator(
                                color = Color.Gray,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(30.dp)
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .padding(10.dp)
                        .background(Color.Transparent, RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                ){
                    if (isConnected) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray
                                        )
                                        .padding(8.dp)
                                        .clickable { if (!isRecording) vm.disconnectDevice() },
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    connectedDevice.polarVariant?.let {
                                        Text(
                                            text = it.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "CONNECTED",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.Green
                                    )
                                }
                            }
                        }
                    }else if (numOfDiscoveredDevices != 0) {
                        for (index in 0 until numOfDiscoveredDevices) {
                            item {
                                val device = discoveredDevices.get(index)
                                if (device.name != null) {
                                    BluetoothDeviceItem(vm, device, connectedDevice)
                                }
                            }
                        }
                    }else{
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray
                                        )
                                        .padding(8.dp)
                                        .clickable {

                                        },

                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBluetoothSearching) "Searching..." else "No Devices Found",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                if(isRecording) {
                    Text(
                        text = accelerometerValues,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Color.Black
                    )
                }
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ){
                    Button(
                        onClick = {
                            vm.setRecordWithBluetoothDevice(false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = if (!recordWithBlueToothDevice) AndroidGreen else Color.DarkGray,
                            containerColor = if (!recordWithBlueToothDevice) Color.DarkGray else Color.Gray
                        ),
                        modifier = Modifier
                            .border(1.dp, ThemeBlack, shape = RoundedCornerShape(10.dp))
                            .weight(1f)
                            .background(Color.Gray, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !(isRecording && (recordWithBlueToothDevice))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.android),
                            contentDescription = "Android",
                            modifier = Modifier
                                .height(70.dp)
                                .aspectRatio(3f / 2f)
                        )
                    }
                    Button(
                        onClick = {
                            vm.setRecordWithBluetoothDevice(true)
                            if (!isConnected){
                                vm.getFoundBluetoothDevices()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = if (recordWithBlueToothDevice) BluetoothBlue else Color.DarkGray,
                            containerColor = if (recordWithBlueToothDevice) Color.DarkGray else Color.Gray
                        ),
                        modifier = Modifier
                            .border(1.dp, ThemeBlack, shape = RoundedCornerShape(10.dp))
                            .weight(1f)
                            .background(Color.Gray, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !(isRecording && !recordWithBlueToothDevice)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bluetooth),
                            contentDescription = "Bluetooth",
                            modifier = Modifier
                                .height(70.dp)
                                .aspectRatio(3f / 2f)
                        )
                    }
                }
                Button(
                    onClick = {
                        if(isRecording){
                            vm.setIsRecording(false)
                            vm.stopAccStreaming()
                        } else {
                            vm.setIsRecording(true)
                            vm.startAccStreaming(settings)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray,
                    ),
                    modifier = Modifier
                        .border(1.dp, ThemeBlack, shape = RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                        .background(Color.Gray, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = if ((recordWithBlueToothDevice && isConnected) || !recordWithBlueToothDevice) true else false
                ) {
                    Text(
                        text = "REC",
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        color = if (isRecording) Color.Red else Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceItem(vm: MoveSenseVM, bluetoothDevice: BluetoothDevice, connectedDevice: ConnectedDevice) {
    var selectedDevice by remember { mutableStateOf(false) }
    if(connectedDevice != null){
        if (connectedDevice.bluetoothVariant?.equals(bluetoothDevice) == true){
            selectedDevice = true
        }
    }
    var textcolor = Color.Gray
    for (pDevice in vm.foundPolarDevices.value){
        if (bluetoothDevice.name == pDevice.name){
            textcolor = Color.White
            break
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.DarkGray
                )
                .padding(8.dp)
                .clickable { vm.connectToDevice(bluetoothDevice) },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = bluetoothDevice.name,
                style = MaterialTheme.typography.titleMedium,
                color = textcolor
            )
            Text(
                text = bluetoothDevice.address,
                style = MaterialTheme.typography.titleMedium,
                color = textcolor
            )
        }
    }
}
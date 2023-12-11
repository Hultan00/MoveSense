package com.example.accmon.ui.screens

import Acc
import android.bluetooth.BluetoothDevice
import android.graphics.Typeface
import android.util.Log
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.LineType
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import com.example.accmon.R
import com.example.accmon.data.ConnectedDevice
import com.example.accmon.data.Gyro
import com.example.accmon.ui.theme.AndroidGreen
import com.example.accmon.ui.theme.BluetoothBlue
import com.example.accmon.ui.theme.StyleBlue
import com.example.accmon.ui.theme.StylePink
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
    val gyroscope = vm.getGyroscope()
    val bluetoothDeviceScanner = vm.getBluetoothScanner()
    val discoveredDevices by vm.foundBluetoothDevices.collectAsState()
    val isBluetoothSearching by vm.isBluetoothSearching.collectAsState()
    val connectedDevice by vm.connectedDevice.collectAsState()
    val polarAccValues by vm.polarAccValues.collectAsState()
    val polarGyroValues by vm.polarGyroValues.collectAsState()

    val numOfDiscoveredDevices by vm.numOfDiscoveredDevices.collectAsState()
    var accelerometerValues by remember { mutableStateOf("No data") }
    var gyroValues by remember { mutableStateOf("No data") }
    var isConnected by remember { mutableStateOf(false) }
    var delayMS by remember { mutableLongStateOf(0) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var accSampleCount by remember { mutableIntStateOf(0) }
    var gyroSampleCount by remember { mutableIntStateOf(0) }
    var hasRecording by remember { mutableStateOf(false) }
    var sampleReferenceTime by remember { mutableLongStateOf(0L) }
    var lastAcc = Acc(0,0,0,-1)

    // Observe changes in accelerometer values
    DisposableEffect(accelerometer) {
        val callback: (Float, Float, Float, Long) -> Unit = { x, y, z, nano ->
            var acc = Acc(0,0,0,-1)
            if (lastAcc.ms >= 0.0){
                acc = Acc(((x / 9.806) * 1000).toInt(), ((y / 9.806) * 1000).toInt(), ((z / 9.806) * 1000).toInt(), (nano - sampleReferenceTime) / 1_000_000, lastAcc)
                lastAcc = acc
            }else{
                acc = Acc(((x / 9.806) * 1000).toInt(), ((y / 9.806) * 1000).toInt(), ((z / 9.806) * 1000).toInt(), (nano - sampleReferenceTime) / 1_000_000)
                lastAcc = acc
            }
            synchronized(polarAccValues) {
                polarAccValues.add(acc)
                Log.d("polarAccValues", "Added ${acc} to polarAccValues")
            }

            // Update the UI with the new accelerometer values
            accelerometerValues = "X: ${
                String.format("%.3f", acc.x * 0.009806).toFloat()
            } Y: ${
                String.format("%.3f", acc.y * 0.009806).toFloat()
            } Z: ${String.format("%.3f", acc.z * 0.009806).toFloat()}\n" +
                    "R: ${String.format("%.3f", acc.r).toFloat()}" +
                    " P: ${String.format("%.3f", acc.p).toFloat()}\n" +
                    "[ms]: ${acc.ms}"
        }

        // Set the callback to be notified when accelerometer values change
        accelerometer.setOnSensorChangedCallback(callback)

        // Remove the callback when the composable is disposed
        onDispose {
            accelerometer.setOnSensorChangedCallback { _, _, _, _ -> /* do nothing */ }
        }
    }

    DisposableEffect(gyroscope) {
        val callback: (Float, Float, Float, Long) -> Unit = { x, y, z, nano ->
            val gyro = Gyro( x, y, z, (nano - sampleReferenceTime) / 1_000_000)
            synchronized(polarGyroValues){
                polarGyroValues.add(gyro)
                Log.d("polarGyroValues", "Added ${gyro} to polarGyroValues")
            }
            // Update the UI with the new accelerometer values
            gyroValues = "X: ${
                String.format("%.3f", gyro.x).toFloat()
            } Y: ${
                String.format("%.3f", gyro.y).toFloat()
            } Z: ${String.format("%.3f", gyro.z).toFloat()}\n" + "[ms]: ${gyro.ms}"
        }

        // Set the callback to be notified when accelerometer values change
        gyroscope.setOnSensorChangedCallback(callback)

        // Remove the callback when the composable is disposed
        onDispose {
            gyroscope.setOnSensorChangedCallback { _, _, _, _ -> /* do nothing */ }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            // Check for internet connectivity
            bluetoothEnabled = bluetoothDeviceScanner.isBluetoothEnabled() == true

            val before = isConnected
            isConnected = ((connectedDevice.bluetoothVariant != null) && bluetoothDeviceScanner.isConnected())
            val after = isConnected
            if (before && !after){
                vm.disconnectDevice()
                vm.stopAccStreaming()
                vm.getFoundBluetoothDevices()
                if(isRecording && recordWithBlueToothDevice){
                    vm.setIsRecording(false)
                }
            }



            if (isRecording){
                synchronized(polarAccValues) {
                    accSampleCount = polarAccValues.size
                    val acc = polarAccValues.get(polarAccValues.size - 1)
                    accelerometerValues = "X: ${
                        String.format("%.3f", acc.x * 0.009806).toFloat()
                    } Y: ${
                        String.format("%.3f", acc.y * 0.009806).toFloat()
                    } Z: ${String.format("%.3f", acc.z * 0.009806).toFloat()}\n" +
                            "R: ${String.format("%.3f", acc.r).toFloat()}" +
                            " P: ${String.format("%.3f", acc.p).toFloat()}\n" +
                            "[ms]: ${acc.ms}"
                }
                synchronized(polarGyroValues) {
                    if (!polarGyroValues.isEmpty()) {
                        gyroSampleCount = polarGyroValues.size
                        val gyro = polarGyroValues.get(polarGyroValues.size - 1)
                        gyroValues = "X: ${
                            String.format("%.3f", gyro.x).toFloat()
                        } Y: ${
                            String.format("%.3f", gyro.y).toFloat()
                        } Z: ${String.format("%.3f", gyro.z).toFloat()}\n" + "[ms]: ${gyro.ms}"
                    }
                }
            }

            // Check internet interval
            delay(100)
        }
    }

    val settings: MutableMap<PolarSensorSetting.SettingType, Int> = mutableMapOf()
    settings[PolarSensorSetting.SettingType.SAMPLE_RATE] = 52
    settings[PolarSensorSetting.SettingType.RESOLUTION] = 16
    settings[PolarSensorSetting.SettingType.RANGE] = 8
    settings[PolarSensorSetting.SettingType.CHANNELS] = 3

    val settingsGyro: MutableMap<PolarSensorSetting.SettingType, Int> = mutableMapOf()
    settingsGyro[PolarSensorSetting.SettingType.SAMPLE_RATE] = 52
    settingsGyro[PolarSensorSetting.SettingType.RESOLUTION] = 16
    settingsGyro[PolarSensorSetting.SettingType.RANGE] = 2000
    settingsGyro[PolarSensorSetting.SettingType.CHANNELS] = 3


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
                    }else if(!bluetoothEnabled){
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
                                        text = "Bluetooth OFF",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.Red
                                    )
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
                Row {
                    Spacer(modifier = Modifier.width(40.dp))
                    Text(
                        text = "Movement Data",
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
                        if (isRecording) {
                            CircularProgressIndicator(
                                color = Color.Gray,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(30.dp)
                            )
                        }
                    }
                }
                if(hasRecording) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(10.dp)
                            .background(Color.Transparent)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        LaunchedEffect(key1 = , block = )
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "X-Acc [g/s^2]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in accSampleCount until accSampleCount + 1) {
                                        AccGraphX(polarAccValues = polarAccValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Y-Acc [g/s^2]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in accSampleCount until accSampleCount + 1) {

                                        AccGraphY(polarAccValues = polarAccValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Z-Acc [g/s^2]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in accSampleCount until accSampleCount + 1) {

                                        AccGraphZ(polarAccValues = polarAccValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Pitch Angle [°∠]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in accSampleCount until accSampleCount + 1) {

                                        AccGraphPitch(polarAccValues = polarAccValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Roll Angle [°∠]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in accSampleCount until accSampleCount + 1) {
                                        AccGraphRoll(polarAccValues = polarAccValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "X-Axis Rotation [°/s]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in gyroSampleCount until gyroSampleCount + 1) {
                                        GyroGraphX(polarGyroValues = polarGyroValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Y-Axis Rotation [°/s]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in gyroSampleCount until gyroSampleCount + 1) {
                                        GyroGraphY(polarGyroValues = polarGyroValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Z-Axis Rotation [°/s]",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    for (count in gyroSampleCount until gyroSampleCount + 1) {
                                        GyroGraphZ(polarGyroValues = polarGyroValues, recordWithBlueToothDevice)
                                    }
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                shape = RoundedCornerShape(10.dp), // Set the corner radius as needed
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = Color.DarkGray,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Debug Data",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = accelerometerValues,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = gyroValues,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.DarkGray
                                )
                                .padding(8.dp),

                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Data",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                        }
                    }
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
                            if (recordWithBlueToothDevice){
                                vm.stopAccStreaming()
                            }else{
                                sampleReferenceTime = 0L
                            }
                        } else if(hasRecording) {
                            hasRecording = false
                        } else {
                            hasRecording = true
                            if (recordWithBlueToothDevice){
                                vm.setIsRecording(true)
                                vm.startAccStreaming(settings, settingsGyro)
                            }else{
                                vm.prepareGraphs()
                                sampleReferenceTime = vm.getCurrentNanoTime()
                                vm.setIsRecording(true)
                            }
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
                    enabled = (recordWithBlueToothDevice && isConnected) || !recordWithBlueToothDevice || (hasRecording && !isRecording)
                ) {
                    Text(
                        text = if (isRecording) "STOP" else if (hasRecording) "RESET" else "REC",
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        color = if (isRecording || hasRecording) Color.Red else Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun AccGraphX(polarAccValues: ArrayList<Acc>, recordWithBluetoothDevice: Boolean){
    if (!polarAccValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarAccValues) {
            val pointsData: List<Point> = polarAccValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.x.toFloat()
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 1
            val steps = 8

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData,
            )
        }
    }
}

@Composable
fun AccGraphY(polarAccValues: ArrayList<Acc>, recordWithBluetoothDevice: Boolean){
    if (!polarAccValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarAccValues) {
            val pointsData: List<Point> = polarAccValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.y.toFloat()
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 1
            val steps = 8

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData,
            )
        }
    }
}

@Composable
fun AccGraphZ(polarAccValues: ArrayList<Acc>, recordWithBluetoothDevice: Boolean){
    if (!polarAccValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarAccValues) {
            val pointsData: List<Point> = polarAccValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.z.toFloat()
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 1
            val steps = 8

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData,
            )
        }
    }
}

@Composable
fun AccGraphPitch(polarAccValues: ArrayList<Acc>, recordWithBluetoothDevice: Boolean){
    if (!polarAccValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarAccValues) {
            val pointsData: List<Point> = polarAccValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.p.toFloat()
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 22.5
            val steps = 4

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData,
            )
        }
    }
}

@Composable
fun AccGraphRoll(polarAccValues: ArrayList<Acc>, recordWithBluetoothDevice: Boolean){
    if (!polarAccValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarAccValues) {
            val pointsData: List<Point> = polarAccValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.r.toFloat()
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 45
            val steps = 4

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData
            )
        }
    }
}

@Composable
fun GyroGraphX(polarGyroValues: ArrayList<Gyro>, recordWithBluetoothDevice: Boolean){
    if (!polarGyroValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarGyroValues) {
            val pointsData: List<Point> = polarGyroValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.x
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 360
            val steps = 6

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData
            )
        }
    }
}

@Composable
fun GyroGraphY(polarGyroValues: ArrayList<Gyro>, recordWithBluetoothDevice: Boolean){
    if (!polarGyroValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarGyroValues) {
            val pointsData: List<Point> = polarGyroValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.y
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 360
            val steps = 6

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData
            )
        }
    }
}

@Composable
fun GyroGraphZ(polarGyroValues: ArrayList<Gyro>, recordWithBluetoothDevice: Boolean){
    if (!polarGyroValues.isEmpty()) {
        var indexIterate = 0
        if (recordWithBluetoothDevice){
            indexIterate = 10
        }else{
            indexIterate = 1
        }
        synchronized(polarGyroValues) {
            val pointsData: List<Point> = polarGyroValues
                .filterIndexed { index, _ -> index % indexIterate == 0 } // Select every tenth element
                .map { acc ->
                    Point(
                        (acc.ms / 1000.0).toFloat(),
                        acc.z
                    )
                }

            val xAxisData = AxisData.Builder()
                .axisStepSize(25.dp)
                .backgroundColor(StyleBlue)
                .steps(pointsData.size - 1)
                .labelData { i -> i.toString() }
                .labelAndAxisLinePadding(5.dp)
                .build()


            val stepSize = 360
            val steps = 6

            val yAxisData = AxisData.Builder()
                .steps(steps * 2)  // Include zero as well
                .backgroundColor(StylePink)
                .labelAndAxisLinePadding(5.dp)
                .labelData { i ->
                    val yValue = (i - steps) * stepSize
                    yValue.toString()  // Convert to String
                }.build()


            val lineChartData = LineChartData(
                linePlotData = LinePlotData(
                    lines = listOf(
                        Line(
                            dataPoints = pointsData,
                            LineStyle(color = ThemeBlue, lineType = LineType.SmoothCurve(isDotted = true)),
                            IntersectionPoint(radius = 0.dp),
                            selectionHighlightPoint = SelectionHighlightPoint(
                                color = Color.Black,
                                radius = 5.dp
                            ),
                            shadowUnderLine = ShadowUnderLine(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        ThemeBlue,
                                        Color.Transparent
                                    )
                                ), alpha = 0.3f
                            ),
                            selectionHighlightPopUp = SelectionHighlightPopUp(
                                backgroundColor = Color.Black,
                                backgroundStyle = Stroke(2f),
                                labelColor = Color.Red,
                                labelTypeface = Typeface.DEFAULT_BOLD
                            )
                        )
                    ),
                ),
                xAxisData = xAxisData,
                yAxisData = yAxisData,
                gridLines = GridLines(),
                backgroundColor = Color.White,
                paddingRight = 0.dp,
                paddingTop = 10.dp,
                bottomPadding = 0.dp
            )
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(0.dp),
                lineChartData = lineChartData
            )
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
            textcolor = StyleBlue
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
                color = if (textcolor.equals(Color.Gray)) textcolor else Color.White
            )
            Text(
                text = if (textcolor.equals(Color.Gray)) "Not Compatible" else "COMPATIBLE",
                style = MaterialTheme.typography.titleMedium,
                color = textcolor
            )
        }
    }
}


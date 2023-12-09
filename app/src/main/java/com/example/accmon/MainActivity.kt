package com.example.accmon

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.example.accmon.ui.theme.AccMonTheme
import com.example.accmon.ui.viewmodels.MoveSenseVM
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.accmon.ui.screens.HomeScreen

class MainActivity : ComponentActivity() {

    val moveSenseViewModel: MoveSenseVM by viewModels {
        MoveSenseVM.createFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccMonTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    initScreenNavigator(moveSenseViewModel)
                }
            }
        }
    }
}

@Composable
fun initScreenNavigator(moveSenseViewModel: MoveSenseVM){

    checkAndRequestBluetoothPermissions(moveSenseViewModel)

    // Instantiate the NavHost and set up navigation
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController, vm = moveSenseViewModel)
        }
    }
}

@Composable
fun checkAndRequestBluetoothPermissions(moveSenseViewModel: MoveSenseVM) {
    val context = LocalContext.current

    val bluetoothPermissions = arrayOf(
        BluetoothDeviceScanner.BLUETOOTH,
        BluetoothDeviceScanner.BLUETOOTH_ADMIN,
        BluetoothDeviceScanner.ACCESS_COARSE_LOCATION,
        BluetoothDeviceScanner.BLUETOOTH_SCAN,
        BluetoothDeviceScanner.BLUETOOTH_CONNECT,
        BluetoothDeviceScanner.ACCESS_FINE_LOCATION
    )

    val permissionsToRequest = bluetoothPermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }.toTypedArray()

    if (permissionsToRequest.isNotEmpty()) {
        // Permission not granted, request them
        ActivityCompat.requestPermissions(
            context as Activity,
            permissionsToRequest,
            BluetoothDeviceScanner.REQUEST_BLUETOOTH_PERMISSION
        )
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AccMonTheme {
        Greeting("Android")
    }
}
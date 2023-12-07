package com.example.accmon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.accmon.R
import com.example.accmon.ui.theme.AndroidGreen
import com.example.accmon.ui.theme.BluetoothBlue
import com.example.accmon.ui.theme.StyleBlue
import com.example.accmon.ui.theme.ThemeBlack
import com.example.accmon.ui.theme.ThemeBlue
import com.example.accmon.ui.viewmodels.MoveSenseVM

@Composable
fun HomeScreen(
    vm: MoveSenseVM,
    navController: NavHostController
){
    val recordWithBlueToothDevice by vm.recordWithBluetoothDevice.collectAsState()
    
    
    // Main Column for HomeScreen
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBlue)
            .padding(0.dp),
        verticalArrangement = Arrangement.Bottom,
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
                Text(
                    text = "Bluetooth Devices",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.Black
                )
                LazyColumn(
                    modifier = Modifier
                        .padding(0.dp)
                        .background(Color.Transparent, RoundedCornerShape(10.dp))
                        .fillMaxWidth()
                        .border(1.dp, ThemeBlack)
                ){

                }
            }
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ){
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ){
                    Button(
                        onClick = {
                            vm.setRecordWithBluetoothDevice(false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = if (!recordWithBlueToothDevice) AndroidGreen else Color.LightGray,
                            containerColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .border(1.dp, ThemeBlack, shape = RoundedCornerShape(10.dp))
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
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
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = if (recordWithBlueToothDevice) BluetoothBlue else Color.LightGray,
                            containerColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .border(1.dp, ThemeBlack, shape = RoundedCornerShape(10.dp))
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp)
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
                        vm.setRecordWithBluetoothDevice(true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray
                    ),
                    modifier = Modifier
                        .border(1.dp, ThemeBlack, shape = RoundedCornerShape(10.dp))
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "REC",
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                        color = Color.Red
                    )
                }
            }
        }
    }
}
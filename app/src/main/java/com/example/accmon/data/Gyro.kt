package com.example.accmon.data

class Gyro {
    val x: Float // pitch/s
    val y: Float // roll/s
    val z: Float // yaw/s

    val xa: Float // current pitch
    val ya: Float // current roll
    val za: Float // current yaw

    val ms: Long

    constructor(x: Float, y: Float, z: Float, ms: Long, lastGyro: Gyro){
        this.x = x
        this.y = y
        this.z = z
        this.xa = lastGyro.xa + (x * ((ms - lastGyro.ms)/1000F))
        this.ya = lastGyro.ya + (y * ((ms - lastGyro.ms)/1000F))
        this.za = lastGyro.za + (z * ((ms - lastGyro.ms)/1000F))
        this.ms = ms
    }

    constructor(x: Float, y: Float, z: Float, xa: Float, ya: Float, za: Float, ms: Long){
        this.x = x
        this.y = y
        this.z = z
        this.xa = xa
        this.ya = ya
        this.za = za
        this.ms = ms
    }
}
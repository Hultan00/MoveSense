package com.example.accmon.data

class Gyro {
    val x: Float
    val y: Float
    val z: Float
    val ms: Long

    constructor(x: Float, y: Float, z: Float, ms: Long){
        this.x = x
        this.y = y
        this.z = z
        this.ms = ms
    }
}
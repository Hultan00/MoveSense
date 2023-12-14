package com.example.accmon.data

import Acc
import android.util.Log

class Fusion {
    val p: Float
    val ms: Long

    constructor(acc: Acc, gyro: Gyro) {
        this.p = complementaryFilter(acc.p, gyro.xa)
        this.ms = acc.ms
    }

    private fun complementaryFilter(accPitch: Float, gyroPitch: Float): Float {
        val alpha = 0.7
        return (alpha * accPitch + (1 - alpha) * gyroPitch).toFloat()
    }
}

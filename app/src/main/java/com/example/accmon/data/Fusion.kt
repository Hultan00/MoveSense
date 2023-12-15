package com.example.accmon.data

import Acc

class Fusion(acc: Acc, gyro: Gyro, alpha: Float) {
    val p: Float
    val ms: Long

    init {
        this.p = complementaryFilter(acc.p, gyro.xa, alpha)
        this.ms = acc.ms
    }

    private fun complementaryFilter(accPitch: Float, gyroPitch: Float, alpha: Float): Float {
        return (alpha * accPitch + (1 - alpha) * gyroPitch)
    }
}

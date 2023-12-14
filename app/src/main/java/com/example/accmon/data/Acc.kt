import java.lang.Math.atan2
import java.lang.Math.sqrt

class Acc {
    val x: Int
    val y: Int
    val z: Int
    val p: Float
    val pf: Float
    val r: Float
    val ms: Long

    constructor(x: Int, y: Int, z: Int, ms: Long) {
        this.x = x
        this.y = y
        this.z = z
        this.ms = ms
        this.p = calculatePitch(this.x, this.y, this.z).toFloat()
        this.pf = calculatePitch(this.x, this.y, this.z).toFloat()
        this.r = calculateRoll(this.x, this.y, this.z).toFloat()
    }

    constructor(x: Int, y: Int, z: Int, p: Float, r: Float, ms: Long) {
        this.x = x
        this.y = y
        this.z = z
        this.ms = ms
        this.p = p
        this.pf = p
        this.r = r
    }

    constructor(x: Int, y: Int, z: Int, ms: Long, lastAcc: Acc) {
        this.x = applyEwmaFilter(x, lastAcc.x)
        this.y = applyEwmaFilter(y, lastAcc.y)
        this.z = applyEwmaFilter(z, lastAcc.z)
        this.p = calculatePitch(this.x, this.y, this.z).toFloat()
        this.pf = calculatePitch(x, y, z).toFloat()
        this.r = calculateRoll(this.x, this.y, this.z).toFloat()
        this.ms = ms
    }

    private fun applyEwmaFilter(current: Int, previous: Int): Int {
        val a = 1

        return (a * current + (1.0 - a) * previous).toInt()
    }

    /*
    private fun calculatePitch(x: Int, y: Int, z: Int): Double {
        return Math.toDegrees(Math.atan2(y.toDouble(), Math.sqrt((x * x + z * z).toDouble())))
    }

    private fun calculateRoll(x: Int, z: Int): Double {
        val roll = Math.toDegrees(Math.atan2((-x).toDouble(), z.toDouble()))

        // Adjust roll to be in the range [-180, 180]
        return if (roll > 180) roll - 360 else if (roll < -180) roll + 360 else roll
    }*/

    private fun calculatePitch(accelX: Int, accelY: Int, accelZ: Int): Double {
        return atan2(accelY.toDouble(), sqrt((accelX * accelX + accelZ * accelZ).toDouble())) * 180 / Math.PI
    }

    private fun calculateRoll(accelX: Int, accelY: Int, accelZ: Int): Double {
        return atan2(-accelX.toDouble(), accelZ.toDouble()) * 180 / Math.PI
    }

}

class Acc {
    val x: Int
    val y: Int
    val z: Int
    val p: Double
    val r: Double
    val ms: Long

    constructor(x: Int, y: Int, z: Int, ms: Long) {
        this.x = x
        this.y = y
        this.z = z
        this.ms = ms
        this.p = calculatePitch(this.x, this.y, this.z)
        this.r = calculateRoll(this.x, this.z)
    }

    constructor(x: Int, y: Int, z: Int, p: Double, r: Double, ms: Long) {
        this.x = x
        this.y = y
        this.z = z
        this.ms = ms
        this.p = p
        this.r = r
    }

    constructor(x: Int, y: Int, z: Int, ms: Long, lastAcc: Acc) {
        this.x = applyEwmaFilter(x, lastAcc.x)
        this.y = applyEwmaFilter(y, lastAcc.y)
        this.z = applyEwmaFilter(z, lastAcc.z)
        this.ms = ms
        this.p = calculatePitch(this.x, this.y, this.z)
        this.r = calculateRoll(this.x, this.z)
    }

    private fun applyEwmaFilter(current: Int, previous: Int): Int {
        val a = 0.05

        return (a * current + (1.0 - a) * previous).toInt()
    }

    private fun calculatePitch(x: Int, y: Int, z: Int): Double {
        return Math.toDegrees(Math.atan2(y.toDouble(), Math.sqrt((x * x + z * z).toDouble())))
    }

    private fun calculateReversedPitch(x: Int, y: Int, z: Int): Double {
        val pitch = Math.toDegrees(Math.atan2(y.toDouble(), Math.sqrt((x * x + z * z).toDouble())))
        // Negate the pitch value
        return -pitch
    }


    private fun calculateRoll(x: Int, z: Int): Double {
        val roll = Math.toDegrees(Math.atan2((-x).toDouble(), z.toDouble()))

        // Adjust roll to be in the range [-180, 180]
        return if (roll > 180) roll - 360 else if (roll < -180) roll + 360 else roll
    }
}

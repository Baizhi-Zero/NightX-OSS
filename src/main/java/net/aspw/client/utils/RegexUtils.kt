package net.aspw.client.utils

import java.math.BigDecimal
import java.math.RoundingMode

object RegexUtils {

    fun round(value: Double, places: Int): Double {
        require(places >= 0)
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).toDouble()
    }
}

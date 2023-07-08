package com.peke.hex.editor.ext

import com.peke.hex.editor.utils.StringUtils
import java.util.Locale

fun Int?.toHexString(length:Int = -1):String{
    val value:Int = this ?: 0
    val hexStr = Integer.toHexString(value).toUpperCase(Locale.ROOT)
    return if (length <= 0) hexStr else StringUtils.addZeroBeforeL(hexStr, length)
}

fun Byte?.toUnsignedInt():Int{
    return if (this == null) 0 else this.toInt() and 0xFF
}

fun Byte?.toHexString(length:Int = -1):String{
    val value:Int = this.toUnsignedInt()
    return value.toHexString(length)
}


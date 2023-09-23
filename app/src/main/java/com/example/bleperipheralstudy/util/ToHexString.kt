package com.example.bleperipheralstudy.util

fun toHexString(data: ByteArray): String {
    val sb = StringBuffer()
    for (b in data) {
        val s = Integer.toHexString(0xff and b.toInt())
        if (s.length == 1) sb.append("0")
        sb.append(s)
    }
    return sb.toString()
}
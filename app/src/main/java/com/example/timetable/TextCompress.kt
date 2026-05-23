package com.example.timetable

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

// сжатие строки в текстовый вид: gzip + base64. короче в 5-10 раз для длинного JSON
object TextCompress {

    fun pack(text: String): String {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun unpack(packed: String): String {
        val bytes = Base64.decode(packed.trim(), Base64.DEFAULT)
        val input = GZIPInputStream(ByteArrayInputStream(bytes))
        return input.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}

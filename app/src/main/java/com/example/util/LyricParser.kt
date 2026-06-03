package com.example.util

import java.util.regex.Pattern

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val words: List<String> = text.split(Regex("\\s+")).filter { it.isNotBlank() }
)

object LyricParser {
    private val LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})](.*)")
    private val LRC_SHORT_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})](.*)")

    fun parse(content: String, durationMs: Long = 0): List<LyricLine> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val parsedLines = mutableListOf<LyricLine>()

        var hasTimestamps = false

        for (line in lines) {
            val matcherLong = LRC_PATTERN.matcher(line)
            if (matcherLong.matches()) {
                hasTimestamps = true
                val min = matcherLong.group(1)?.toLongOrNull() ?: 0L
                val sec = matcherLong.group(2)?.toLongOrNull() ?: 0L
                val hun = matcherLong.group(3) ?: "00"
                
                // Adjust for 2-digit vs 3-digit hundredths/milliseconds
                val msSuffix = if (hun.length == 2) hun.toLong() * 10 else hun.toLong()
                val timestamp = (min * 60 + sec) * 1000 + msSuffix
                val text = matcherLong.group(4)?.trim() ?: ""
                parsedLines.add(LyricLine(timestamp, text))
                continue
            }

            val matcherShort = LRC_SHORT_PATTERN.matcher(line)
            if (matcherShort.matches()) {
                hasTimestamps = true
                val min = matcherShort.group(1)?.toLongOrNull() ?: 0L
                val sec = matcherShort.group(2)?.toLongOrNull() ?: 0L
                val timestamp = (min * 60 + sec) * 1000
                val text = matcherShort.group(3)?.trim() ?: ""
                parsedLines.add(LyricLine(timestamp, text))
            }
        }

        if (hasTimestamps) {
            return parsedLines.sortedBy { it.timestampMs }
        }

        // Fallback: If no timestamps, distribute evenly over duration (default 60s if duration is 0)
        val songDuration = if (durationMs > 0) durationMs else 60000L
        if (lines.isNotEmpty()) {
            val interval = songDuration / lines.size
            return lines.mapIndexed { index, text ->
                LyricLine(
                    timestampMs = index * interval,
                    text = text
                )
            }
        }

        return emptyList()
    }
}

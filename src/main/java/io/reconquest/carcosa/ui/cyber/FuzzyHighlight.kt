package io.reconquest.carcosa.ui.cyber

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.util.Locale

fun fuzzyMatchPositions(query: String, candidate: String): List<Int>? {
    val needle = query.trim().lowercase(Locale.getDefault())
    if (needle.isEmpty()) {
        return emptyList()
    }

    val haystack = candidate.lowercase(Locale.getDefault())
    val positions = mutableListOf<Int>()
    var cursor = 0

    for (char in needle) {
        val found = haystack.indexOf(char, startIndex = cursor)
        if (found == -1) {
            return null
        }
        positions += found
        cursor = found + 1
    }

    return positions
}

fun fuzzyScore(query: String, candidate: String): Int {
    val positions = fuzzyMatchPositions(query, candidate) ?: return Int.MAX_VALUE
    if (positions.isEmpty()) {
        return 0
    }

    val span = positions.last() - positions.first()
    val gaps = positions.zipWithNext().sumOf { (left, right) -> right - left - 1 }
    val prefix = positions.first()
    return span * 4 + gaps * 2 + prefix
}

fun neonHighlight(
    text: String,
    query: String,
    baseColor: Color,
    highlightColor: Color,
): AnnotatedString {
    val positions = fuzzyMatchPositions(query, text).orEmpty().toSet()

    return buildAnnotatedString {
        text.forEachIndexed { index, char ->
            if (positions.isNotEmpty() && index in positions) {
                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.Black)) {
                    append(char)
                }
            } else {
                withStyle(SpanStyle(color = baseColor)) {
                    append(char)
                }
            }
        }
    }
}

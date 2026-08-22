package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class NumberCommaVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        
        var out = ""
        val parts = originalText.split(".")
        val intPart = parts[0]
        val fraction = if (parts.size > 1) originalText.substring(intPart.length) else ""
        
        var commaCount = 0
        val commaPositions = mutableListOf<Int>()
        
        for (i in intPart.indices) {
            out += intPart[i]
            val digitsLeft = intPart.length - 1 - i
            if (digitsLeft > 0 && digitsLeft % 3 == 0) {
                out += ","
                commaCount++
                commaPositions.add(i + 1)
            }
        }
        out += fraction
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= intPart.length) {
                    var commas = 0
                    for (pos in commaPositions) {
                        if (offset > pos) commas++
                    }
                    return offset + commas
                }
                return offset + commaCount
            }

            override fun transformedToOriginal(offset: Int): Int {
                var commas = 0
                var i = 0
                while (i < offset && i < out.length) {
                    if (out[i] == ',') commas++
                    i++
                }
                return offset - commas
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

package com.tfg.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tfg.core.ui.theme.*

// ─── Syntax colours (Dracula-inspired) ─────────────────────────────────────
private val SynKeyword  = Color(0xFFFF79C6)  // pink   – val, var, fun…
private val SynString   = Color(0xFFF1FA8C)  // yellow – "strings"
private val SynNumber   = Color(0xFFBD93F9)  // purple – numbers
private val SynComment  = Color(0xFF6272A4)  // muted  – // comments
private val SynFunction = Color(0xFF50FA7B)  // green  – funcCalls()
private val SynType     = Color(0xFF8BE9FD)  // cyan   – Int, Double…
private val SynStrategy = Color(0xFFFFB86C)  // orange – sma, buy, rsi…
private val SynError    = Color(0xFFFF5555)  // red    – unmatched brackets

// ─── Token types ─────────────────────────────────────────────────────────
private enum class TokType { KEYWORD, TYPE, STRING, NUMBER, COMMENT, FUNCTION, STRATEGY }

private val KEYWORDS = setOf(
    "val", "var", "fun", "if", "else", "when", "return", "for", "while", "do",
    "in", "is", "as", "class", "object", "import", "package", "override",
    "private", "public", "internal", "data", "const", "by", "lazy", "suspend",
    "open", "abstract", "interface", "enum", "companion", "init", "get", "set",
    "this", "it", "super", "throw", "try", "catch", "finally", "break",
    "continue", "true", "false", "null", "and", "or", "not"
)

private val TYPES = setOf(
    "Int", "Long", "Double", "Float", "String", "Boolean",
    "List", "Map", "Set", "Unit", "Any", "Nothing", "Pair", "Triple", "Array"
)

private val STRATEGY_WORDS = setOf(
    "strategy", "indicator", "condition", "action", "buy", "sell", "signal",
    "entry", "exit", "stop", "target", "trailing", "crossover", "crossunder",
    "sma", "ema", "rsi", "macd", "bollinger", "grid", "volume", "close",
    "open", "high", "low", "price", "candle", "period", "length", "threshold"
)

// ─── Tokenizer ────────────────────────────────────────────────────────────
private data class Token(val start: Int, val end: Int, val type: TokType)

private fun tokenize(code: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    while (i < code.length) {
        when {
            // Line comment
            i + 1 < code.length && code[i] == '/' && code[i + 1] == '/' -> {
                val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                tokens += Token(i, end, TokType.COMMENT)
                i = end
            }
            // String literal
            code[i] == '"' -> {
                var j = i + 1
                while (j < code.length && code[j] != '"' && code[j] != '\n') {
                    if (code[j] == '\\') j++
                    j++
                }
                if (j < code.length && code[j] == '"') j++
                tokens += Token(i, j, TokType.STRING)
                i = j
            }
            // Number
            code[i].isDigit() -> {
                var j = i
                while (j < code.length && (code[j].isDigit() || code[j] == '.' || code[j] in "fFLl")) j++
                tokens += Token(i, j, TokType.NUMBER)
                i = j
            }
            // Word (keyword / type / strategy / function call)
            code[i].isLetter() || code[i] == '_' -> {
                var j = i
                while (j < code.length && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val word = code.substring(i, j)
                val type: TokType? = when {
                    word in KEYWORDS -> TokType.KEYWORD
                    word in TYPES -> TokType.TYPE
                    word.lowercase() in STRATEGY_WORDS -> TokType.STRATEGY
                    else -> {
                        // peek past whitespace for '('
                        var k = j
                        while (k < code.length && code[k] == ' ') k++
                        if (k < code.length && code[k] == '(') TokType.FUNCTION else null
                    }
                }
                if (type != null) tokens += Token(i, j, type)
                i = j
            }
            else -> i++
        }
    }
    return tokens
}

// ─── Live error detection: unmatched brackets ────────────────────────────
private fun errorPositions(code: String): Set<Int> {
    val errors = mutableSetOf<Int>()
    val stack = ArrayDeque<Pair<Char, Int>>()
    val matching = mapOf(')' to '(', '}' to '{', ']' to '[')
    for ((idx, ch) in code.withIndex()) {
        when (ch) {
            '(', '{', '[' -> stack.addLast(ch to idx)
            ')', '}', ']' -> {
                if (stack.isEmpty() || stack.last().first != matching[ch]) {
                    errors += idx
                } else {
                    stack.removeLast()
                }
            }
        }
    }
    stack.forEach { errors += it.second }
    return errors
}

// ─── VisualTransformation ─────────────────────────────────────────────────
private class KotlinSyntaxTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val code = text.text
        val tokens = tokenize(code)
        val errors = errorPositions(code)

        val annotated = buildAnnotatedString {
            append(code)
            for (tok in tokens) {
                val style = when (tok.type) {
                    TokType.KEYWORD  -> SpanStyle(color = SynKeyword,  fontWeight = FontWeight.Bold)
                    TokType.TYPE     -> SpanStyle(color = SynType)
                    TokType.STRING   -> SpanStyle(color = SynString)
                    TokType.NUMBER   -> SpanStyle(color = SynNumber)
                    TokType.COMMENT  -> SpanStyle(color = SynComment, fontStyle = FontStyle.Italic)
                    TokType.FUNCTION -> SpanStyle(color = SynFunction)
                    TokType.STRATEGY -> SpanStyle(color = SynStrategy, fontWeight = FontWeight.SemiBold)
                }
                addStyle(style, tok.start, tok.end)
            }
            // Red underline on unmatched brackets
            for (pos in errors) {
                addStyle(
                    SpanStyle(color = SynError, textDecoration = TextDecoration.Underline),
                    pos, pos + 1
                )
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

// ─── Public composable ────────────────────────────────────────────────────
@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 200.dp
) {
    val transformation = remember { KotlinSyntaxTransformation() }
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .border(
                width = 1.dp,
                color = if (isFocused) AccentBlue else DarkBorder,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = TextPrimary
            ),
            cursorBrush = SolidColor(AccentBlue),
            visualTransformation = transformation
        )
    }
}

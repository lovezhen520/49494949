package com.webide.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.webide.app.domain.model.FileType
import kotlinx.coroutines.delay

@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    fileType: FileType,
    fileName: String,
    modifier: Modifier = Modifier
) {
    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color(0xFF007ACC),
        backgroundColor = Color(0xFF007ACC).copy(alpha = 0.3f)
    )

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(code))
    }

    var lineCount by remember { mutableStateOf(code.lines().size) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(code) {
        if (textFieldValue.text != code) {
            textFieldValue = TextFieldValue(code)
            lineCount = code.lines().size
        }
    }

    LaunchedEffect(textFieldValue.text) {
        delay(10)
        lineCount = textFieldValue.text.lines().size
    }

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp)
                    .background(Color(0xFF2D2D2D)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            color = Color(0xFF6B6B6B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp, top = 4.dp, bottom = 4.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = Color(0xFF3C3C3C)
            )

            Box(
                modifier = Modifier.weight(1f)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        onCodeChange(newValue.text)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFFD4D4D4),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF007ACC)),
                    onTextLayout = { layoutResult = it },
                    decorationBox = { innerTextField ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "${lineCount} lines",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                BasicTextField(
                    value = highlightSyntax(textFieldValue.text, fileType),
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = 32.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.Transparent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    enabled = false,
                    readOnly = true
                )
            }
        }
    }
}

fun highlightSyntax(code: String, fileType: FileType): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = code.lines()

    when (fileType) {
        FileType.HTML -> highlightHtml(builder, lines)
        FileType.CSS -> highlightCss(builder, lines)
        FileType.JS -> highlightJs(builder, lines)
        FileType.JSON -> highlightJson(builder, lines)
        else -> {
            lines.forEachIndexed { index, line ->
                builder.append(line)
                if (index < lines.size - 1) {
                    builder.append("\n")
                }
            }
        }
    }

    return builder.toAnnotatedString()
}

private fun highlightHtml(builder: AnnotatedString.Builder, lines: List<String>) {
    val tagRegex = Regex("(<[^>]+>)")
    val attrRegex = Regex("([a-zA-Z-]+)\\s*=")
    val stringRegex = Regex("\"[^\"]*\"|'[^']*'")

    lines.forEachIndexed { index, line ->
        var remaining = line
        var lastIndex = 0

        tagRegex.findAll(line).forEach { match ->
            if (match.range.first > lastIndex) {
                builder.append(line.substring(lastIndex, match.range.first))
            }

            val tag = match.value
            if (tag.startsWith("</")) {
                builder.pushStyle(SpanStyle(color = Color(0xFF569CD6)))
            } else if (tag.startsWith("<!")) {
                builder.pushStyle(SpanStyle(color = Color(0xFF6A9955)))
            } else {
                builder.pushStyle(SpanStyle(color = Color(0xFF569CD6)))
            }
            builder.append(tag)
            builder.pop()
            lastIndex = match.range.last + 1
        }

        if (lastIndex < line.length) {
            builder.append(line.substring(lastIndex))
        }

        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }
}

private fun highlightCss(builder: AnnotatedString.Builder, lines: List<String>) {
    val selectorRegex = Regex("^\\s*([a-zA-Z][a-zA-Z0-9\\-_]*|\\.[a-zA-Z][a-zA-Z0-9\\-_]*|#[a-zA-Z0-9]+)")
    val propertyRegex = Regex("(\\s*[a-zA-Z-]+)\\s*:")
    val valueRegex = Regex(":\\s*([^;]+)")
    val commentRegex = Regex("/\\*.*?\\*/")

    lines.forEachIndexed { index, line ->
        var remaining = line
        var lastIndex = 0

        commentRegex.findAll(line).forEach { match ->
            if (match.range.first > lastIndex) {
                builder.append(line.substring(lastIndex, match.range.first))
            }
            builder.pushStyle(SpanStyle(color = Color(0xFF6A9955)))
            builder.append(match.value)
            builder.pop()
            lastIndex = match.range.last + 1
        }

        if (lastIndex < line.length) {
            val remainingLine = line.substring(lastIndex)
            
            selectorRegex.find(remainingLine)?.let { match ->
                builder.pushStyle(SpanStyle(color = Color(0xFFDCDCAA), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                builder.append(match.value)
                builder.pop()
                lastIndex += match.range.last + 1
            }

            propertyRegex.findAll(remainingLine).forEach { match ->
                val prop = match.groupValues[1].trim()
                val propStart = match.range.first
                if (propStart > 0) {
                    builder.append(remainingLine.substring(lastIndex - line.length + lastIndex, propStart))
                }
                builder.pushStyle(SpanStyle(color = Color(0xFF9CDCFE)))
                builder.append(prop)
                builder.append(":")
                builder.pop()
                lastIndex = match.range.last + 1
            }

            valueRegex.findAll(remainingLine).forEach { match ->
                val value = match.groupValues[1].trim()
                if (value.startsWith("#") || value.matches(Regex("[0-9.]+(px|em|rem|%|vh|vw)?$"))) {
                    builder.pushStyle(SpanStyle(color = Color(0xFFB5CEA8)))
                } else if (value.startsWith("\"") || value.startsWith("'")) {
                    builder.pushStyle(SpanStyle(color = Color(0xFFCE9178)))
                } else {
                    builder.pushStyle(SpanStyle(color = Color(0xFFCE9178)))
                }
                builder.append(":")
                builder.append(value)
                builder.pop()
            }
        }

        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }
}

private fun highlightJs(builder: AnnotatedString.Builder, lines: List<String>) {
    val keywords = setOf(
        "var", "let", "const", "function", "return", "if", "else", "for", "while",
        "class", "extends", "new", "this", "import", "export", "default", "from",
        "async", "await", "try", "catch", "throw", "typeof", "instanceof", "null",
        "undefined", "true", "false", "switch", "case", "break", "continue", "do"
    )
    val commentRegex = Regex("//.*$")
    val stringRegex = Regex("\"[^\"]*\"|'[^']*'|`[^`]*`")
    val numberRegex = Regex("\\b\\d+\\.?\\d*\\b")
    val functionRegex = Regex("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(")

    lines.forEachIndexed { index, line ->
        var remaining = line
        var lastIndex = 0

        commentRegex.find(line)?.let { match ->
            if (match.range.first > lastIndex) {
                remaining = line.substring(lastIndex, match.range.first)
                builder.append(remaining)
            }
            builder.pushStyle(SpanStyle(color = Color(0xFF6A9955)))
            builder.append(match.value)
            builder.pop()
            lastIndex = line.length
        }

        if (lastIndex < line.length) {
            remaining = line.substring(lastIndex)
            
            stringRegex.findAll(remaining).forEach { match ->
                if (match.range.first > 0) {
                    processNonMatch(builder, remaining.substring(0, match.range.first), keywords, functionRegex)
                }
                builder.pushStyle(SpanStyle(color = Color(0xFFCE9178)))
                builder.append(match.value)
                builder.pop()
                remaining = remaining.substring(match.range.last + 1)
            }

            processNonMatch(builder, remaining, keywords, functionRegex)
        }

        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }
}

private fun processNonMatch(
    builder: AnnotatedString.Builder,
    text: String,
    keywords: Set<String>,
    functionRegex: Regex
) {
    val parts = text.split(Regex("(?<=[\\s{}();,:])|(?=[\\s{}();,:])"))
    parts.forEach { part ->
        if (part.trim().isEmpty()) {
            builder.append(part)
            return@forEach
        }

        if (part.matches(Regex("\\b\\d+\\.?\\d*\\b"))) {
            builder.pushStyle(SpanStyle(color = Color(0xFFB5CEA8)))
            builder.append(part)
            builder.pop()
        } else if (keywords.contains(part)) {
            builder.pushStyle(SpanStyle(color = Color(0xFF569CD6), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            builder.append(part)
            builder.pop()
        } else {
            functionRegex.find(part)?.let { match ->
                val funcName = match.groupValues[1]
                builder.pushStyle(SpanStyle(color = Color(0xFFDCDCAA)))
                builder.append(funcName)
                builder.pop()
                builder.append(part.substring(funcName.length))
            } ?: run {
                builder.append(part)
            }
        }
    }
}

private fun highlightJson(builder: AnnotatedString.Builder, lines: List<String>) {
    val keyRegex = Regex("\"([^\"]*)\":")
    val stringRegex = Regex(":\"([^\"]*)\"")
    val numberRegex = Regex(":\\s*(-?\\d+\\.?\\d*)")
    val boolRegex = Regex(":\\s*(true|false)")
    val nullRegex = Regex(":\\s*null")

    lines.forEachIndexed { index, line ->
        var remaining = line
        var lastIndex = 0

        keyRegex.findAll(line).forEach { match ->
            if (match.range.first > lastIndex) {
                builder.append(line.substring(lastIndex, match.range.first))
            }
            builder.pushStyle(SpanStyle(color = Color(0xFF9CDCFE)))
            builder.append(match.value)
            builder.pop()
            lastIndex = match.range.last + 1
        }

        if (lastIndex < line.length) {
            val remainingLine = line.substring(lastIndex)

            stringRegex.findAll(remainingLine).forEach { match ->
                builder.pushStyle(SpanStyle(color = Color(0xFFCE9178)))
                builder.append(":")
                builder.append("\"")
                builder.append(match.groupValues[1])
                builder.append("\"")
                builder.pop()
            }

            numberRegex.findAll(remainingLine).forEach { match ->
                builder.pushStyle(SpanStyle(color = Color(0xFFB5CEA8)))
                builder.append(": ")
                builder.append(match.groupValues[1])
                builder.pop()
            }

            boolRegex.findAll(remainingLine).forEach { match ->
                builder.pushStyle(SpanStyle(color = Color(0xFF569CD6)))
                builder.append(": ")
                builder.append(match.groupValues[1])
                builder.pop()
            }

            nullRegex.findAll(remainingLine).forEach { match ->
                builder.pushStyle(SpanStyle(color = Color(0xFF569CD6)))
                builder.append(": ")
                builder.append("null")
                builder.pop()
            }

            if (!stringRegex.containsMatchIn(remainingLine) && 
                !numberRegex.containsMatchIn(remainingLine) &&
                !boolRegex.containsMatchIn(remainingLine) &&
                !nullRegex.containsMatchIn(remainingLine)) {
                builder.append(remainingLine)
            }
        }

        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }
}

package com.webide.app.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.webide.app.domain.model.FileType

class CodeEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private var fileType: FileType = FileType.HTML
    private var syntaxColors = SyntaxColors()

    init {
        setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        setBackgroundColor(Color(0xFF1E1E1E).toArgb())
        setTextColor(Color(0xFFD4D4D4).toArgb())
        setLineSpacing(4f, 1f)
        gravity = Gravity.TOP or Gravity.START
        inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or 
                   InputType.TYPE_TEXT_FLAG_MULTI_LINE or 
                   InputType.TYPE_CLASS_TEXT
        
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applySyntaxHighlighting()
            }
        })
    }

    fun setFileType(type: FileType) {
        fileType = type
        applySyntaxHighlighting()
    }

    fun setSyntaxColors(colors: SyntaxColors) {
        syntaxColors = colors
        applySyntaxHighlighting()
    }

    private fun applySyntaxHighlighting() {
        val text = text ?: return
        val spannable = SpannableStringBuilder(text)
        
        spannable.clearSpans()
        
        when (fileType) {
            FileType.HTML -> highlightHTML(spannable)
            FileType.CSS -> highlightCSS(spannable)
            FileType.JS -> highlightJavaScript(spannable)
            FileType.JSON -> highlightJSON(spannable)
            FileType.OTHER -> {}
        }
        
        setText(spannable)
        setSelection(text.length)
    }

    private fun highlightHTML(spannable: SpannableStringBuilder) {
        val htmlTags = Regex("</?[a-zA-Z0-9]+[^>]*>")
        htmlTags.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.tag),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val attributes = Regex("\\s[a-zA-Z-]+(?==)")
        attributes.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.attribute),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val attributeValues = Regex("\"[^\"]*\"|'[^']*'")
        attributeValues.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.string),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val comments = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        comments.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.comment),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun highlightCSS(spannable: SpannableStringBuilder) {
        val selectors = Regex("[.#]?[a-zA-Z_-][^{}]+(?=\\s*\\{)")
        selectors.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.selector),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val properties = Regex("[a-zA-Z-]+(?=\\s*:)")
        properties.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.property),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val values = Regex(":(.*?);", RegexOption.DOT_MATCHES_ALL)
        values.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.value),
                match.range.first + 1,
                match.range.last,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val comments = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        comments.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.comment),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun highlightJavaScript(spannable: SpannableStringBuilder) {
        val keywords = Regex("\\b(?:function|var|let|const|if|else|for|while|do|switch|case|break|continue|return|try|catch|finally|throw|new|this|class|extends|import|export|default|async|await|true|false|null|undefined)\\b")
        keywords.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.keyword),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val strings = Regex("\"[^\"]*\"|'[^']*'|`[^`]*`")
        strings.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.string),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val comments = Regex("//.*$|/\\*.*?\\*/", RegexOption.MULTILINE or RegexOption.DOT_MATCHES_ALL)
        comments.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.comment),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val numbers = Regex("\\b\\d+\\.?\\d*\\b")
        numbers.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.number),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun highlightJSON(spannable: SpannableStringBuilder) {
        val keys = Regex("\"[^\"]*\"(?=\\s*:)")
        keys.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.attribute),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val strings = Regex("(?<=:\\s*)\"[^\"]*\"")
        strings.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.string),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val numbers = Regex("\\b\\d+\\.?\\d*\\b")
        numbers.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.number),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val booleans = Regex("\\b(?:true|false|null)\\b")
        booleans.findAll(spannable).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(syntaxColors.keyword),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun Int.dpToPx(): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }
}

data class SyntaxColors(
    val tag: Int = Color(0xFF569CD6).toArgb(),
    val attribute: Int = Color(0xFF9CDCFE).toArgb(),
    val string: Int = Color(0xFFCE9178).toArgb(),
    val comment: Int = Color(0xFF6A9955).toArgb(),
    val keyword: Int = Color(0xFF569CD6).toArgb(),
    val selector: Int = Color(0xFFD7BA7D).toArgb(),
    val property: Int = Color(0xFF9CDCFE).toArgb(),
    val value: Int = Color(0xFFCE9178).toArgb(),
    val number: Int = Color(0xFFB5CEA8).toArgb()
)

@Composable
fun CodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    fileType: FileType,
    modifier: Modifier = Modifier
) {
    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color(0xFF007ACC),
        backgroundColor = Color(0xFF007ACC).copy(alpha = 0.4f)
    )

    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E)),
            factory = { context ->
                CodeEditorView(context).apply {
                    setText(code)
                    setFileType(fileType)
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            onCodeChange(s.toString())
                        }
                    })
                }
            },
            update = { view ->
                if (view.text.toString() != code) {
                    view.setText(code)
                }
                view.setFileType(fileType)
            }
        )
    }
}

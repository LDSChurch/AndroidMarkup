package com.devbrackets.android.androidmarkup.parser.markdown

import android.text.Spanned
import com.devbrackets.android.androidmarkup.parser.core.MarkupDocument
import com.devbrackets.android.androidmarkup.parser.core.MarkupElement
import com.devbrackets.android.androidmarkup.parser.core.SpanType
import org.commonmark.parser.Parser

/**
 * This markdown document and associated parsing follows the
 * spec defined by [spec.commonmark.org/0.25/](http://spec.commonmark.org/0.25/)
 */
open class MarkdownDocument : MarkupDocument {

    constructor(spanned: Spanned) : super(spanned)

    constructor(markdown: String) : super() {
        val parser = Parser.Builder().build()
        val converter = MarkdownDocumentConverter.Builder().build();

        rootElement.children.clear()
        rootElement.addChild(converter.convert(parser.parse(markdown)))
    }

    open fun toMarkdown(): String {
        val builder: StringBuilder = StringBuilder()
        toMarkdown(rootElement, builder)

        return builder.toString()
    }

    protected open fun toMarkdown(element: MarkupElement, builder: StringBuilder) {
        when (element.spanType) {
            SpanType.UNKNOWN -> convertChildren(element, builder)
            SpanType.TEXT -> convertTextSpan(element, builder)
            SpanType.BOLD -> convertBoldSpan(element, builder)
            SpanType.ITALIC -> convertItalicSpan(element, builder)
            SpanType.ORDERED_LIST -> convertOrderedListSpan(element, builder)
            SpanType.UNORDERED_LIST -> convertUnOrderedListSpan(element, builder)
        }
    }

    protected open fun convertTextSpan(element: MarkupElement, builder: StringBuilder) {
        builder.append(escapeString(element.text.orEmpty()))
    }

    protected open fun convertBoldSpan(element: MarkupElement, builder: StringBuilder) {
        builder.append(BOLD_TAG)
        if (!convertChildren(element, builder)) {
            builder.append(escapeString(element.text.orEmpty()))
        }
        builder.append(BOLD_TAG)
    }

    protected open fun convertItalicSpan(element: MarkupElement, builder: StringBuilder) {
        builder.append(ITALICS_TAG)
        if (!convertChildren(element, builder)) {
            builder.append(escapeString(element.text.orEmpty()))
        }
        builder.append(ITALICS_TAG)
    }

    protected open fun convertOrderedListSpan(element: MarkupElement, builder: StringBuilder) {
        val tempBuilder = StringBuilder()
        convertChildren(element, tempBuilder)

        val lines = tempBuilder.toString().orEmpty().split("\n")
        for (i in lines.indices) {
            builder.append(ORDERED_LIST_ITEM)
            builder.append(lines[i])
            if (i != lines.size -1) {
                builder.appendln()
            }
        }

        if (lines.isNotEmpty()) {
            builder.appendln().appendln()
        }
    }

    protected open fun convertUnOrderedListSpan(element: MarkupElement, builder: StringBuilder) {
        val tempBuilder = StringBuilder()
        convertChildren(element, tempBuilder)

        val lines = tempBuilder.toString().orEmpty().split("\n")
        for (i in lines.indices) {
            builder.append(UNORDERED_LIST_ITEM)
            builder.append(lines[i])
            if (i != lines.size -1) {
                builder.appendln()
            }
        }

        if (lines.isNotEmpty()) {
            builder.appendln().appendln()
        }
    }

    protected open fun convertChildren(element: MarkupElement, builder: StringBuilder) : Boolean {
        if (element.children.isEmpty()) {
            return false
        }

        for (child in element.children) {
            toMarkdown(child, builder)
        }

        return true
    }

    /**
     * Makes sure special ASCII punctuation and other formatting such
     * as new lines are properly escaped for persistence.  the ASCII
     * punctuation should always be escaped, however the new lines are
     * persisted because this is used for a WYSIWYG editor.
     */
    protected open fun escapeString(unescapedString: String): String {
        var temp = unescapedString.replace(ESCAPE_CHARS_REGEX, transform = {"\\${it.value}"})
        temp = temp.replace("\n", "&nbsp;  \n")
        return temp;
    }

    companion object {
        const val BOLD_TAG: String = "**"
        const val ITALICS_TAG: String = "_"
        const val ORDERED_LIST_ITEM = "0. "
        const val UNORDERED_LIST_ITEM = "* "

        //Also known as ASCII punctuation characters
        val ESCAPE_CHARS_REGEX = Regex("[\\!\"#\\$%&'\\(\\)\\*\\+,\\-\\./:;\\<\\=\\>\\?@\\[\\]\\^_`\\{\\|\\}~\\\\]")
    }
}
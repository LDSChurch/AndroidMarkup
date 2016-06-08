package com.devbrackets.android.androidmarkup.widget

import android.content.Context
import android.graphics.Typeface
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.AttributeSet
import com.devbrackets.android.androidmarkup.parser.core.MarkupParser
import com.devbrackets.android.androidmarkup.parser.core.SpanType
import com.devbrackets.android.androidmarkup.parser.html.HtmlParser
import com.devbrackets.android.androidmarkup.text.style.ListSpan
import java.util.*

/**
 * A WYSIWYG EditText for Markup languages such as HTML or
 * Markdown.  This leaves the UI up to the implementing application.
 */
open class MarkupEditText : AppCompatEditText {
    var markupParser: MarkupParser = HtmlParser()

    @JvmField
    var markupControlsCallbacks: MarkupControlsCallbacks? = null

    var boldToggled = false
    var italicToggled = false
    var orderedListToggled = false
    var unorderedListToggled = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

            val toggleBold = getOverlappingStyleSpans(text, selStart, selEnd, Typeface.BOLD).isNotEmpty()
            boldToggled = toggleBold
            val toggleItalic = getOverlappingStyleSpans(text, selStart, selEnd, Typeface.ITALIC).isNotEmpty()
            italicToggled = toggleItalic

            markupControlsCallbacks?.boldToggled(toggleBold)
            markupControlsCallbacks?.italicToggled(toggleItalic)
    }

    open fun toggleBold() {
        if (hasSelection()) {
            markupParser.updateSpan(text, SpanType.BOLD, selectionStart, selectionEnd)
            onSelectionChanged(selectionStart, selectionEnd)
        } else {
            boldToggled = !boldToggled
            markupControlsCallbacks?.boldToggled(boldToggled)
        }
    }

    open fun toggleItalics() {
        if (hasSelection()) {
            markupParser.updateSpan(text, SpanType.ITALIC, selectionStart, selectionEnd)
            onSelectionChanged(selectionStart, selectionEnd)
        } else {
            italicToggled = !italicToggled
        }
    }

    open fun toggleOrderedList() {
        if (hasSelection()) {
            markupParser.updateSpan(text, SpanType.ORDERED_LIST, selectionStart, selectionEnd)
            onSelectionChanged(selectionStart, selectionEnd)
        } else {
            orderedListToggled = !orderedListToggled
        }
    }

    open fun toggleUnOrderedList() {
        if (hasSelection()) {
            markupParser.updateSpan(text, SpanType.UNORDERED_LIST, selectionStart, selectionEnd)
            onSelectionChanged(selectionStart, selectionEnd)
        } else {
            unorderedListToggled = !unorderedListToggled
        }
    }

    open fun getMarkup() : String {
        return markupParser.fromSpanned(text)
    }

    open fun setMarkup(markup: String) {
        setText(markupParser.toSpanned(markup))
    }

    protected fun init() {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(text: Editable?) {
                // Do nothing
            }

            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                val spannable = text as Spannable
                handleStyledTextChanged(spannable, start, start + count)

                markupParser.updateListItems(text, start, start + count)
            }
        })
    }

    protected fun handleStyledTextChanged(spannable: Spannable, start: Int, end: Int) {
        val boldSpans = getOverlappingStyleSpans(spannable, start, end, Typeface.BOLD)
        if (boldSpans.isEmpty() && boldToggled) {
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        } else if (boldSpans.isNotEmpty() && !boldToggled) {
            for (span in boldSpans) {
                updateSpanPositions(end, span, spannable, start)
            }
        }

        val italicSpans = getOverlappingStyleSpans(spannable, start, end, Typeface.ITALIC)
        if (italicSpans.isEmpty() && italicToggled) {
            spannable.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        } else if (italicSpans.isNotEmpty() && !italicToggled) {
            for (span in italicSpans) {
                updateSpanPositions(end, span, spannable, start)
            }
        }
    }

    private fun updateSpanPositions(end: Int, span: StyleSpan, spannable: Spannable, start: Int) {
        val spanStart = spannable.getSpanStart(span)
        val spanEnd = spannable.getSpanEnd(span)
        spannable.setSpan(span, spanStart, start, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        spannable.setSpan(span.style, end, spanEnd, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    protected fun getOverlappingStyleSpans(spannable: Spannable, selectionStart: Int, selectionEnd: Int, style: Int): List<StyleSpan> {
        var selectionStartPosition = selectionStart
        var selectionEndPosition = selectionEnd
        //Makes sure the start and end are contained in the spannable
        selectionStartPosition = if (selectionStartPosition < 0) 0 else selectionStartPosition
        selectionEndPosition = if (selectionEndPosition >= spannable.length) spannable.length - 1 else selectionEndPosition

        val spans = LinkedList(Arrays.asList(*spannable.getSpans(selectionStartPosition, selectionEndPosition, StyleSpan::class.java)))

        //Filters out the non-matching types
        val iterator = spans.iterator()
        while (iterator.hasNext()) {
            val span = iterator.next()
            if (span.style != style) {
                iterator.remove()
            }
        }

        return spans
    }

    protected fun getOverlappingListSpans(spannable: Spannable, selectionStart: Int, selectionEnd: Int): List<ListSpan> {
        var selectionStartPosition = selectionStart
        var selectionEndPosition = selectionEnd
        //Makes sure the start and end are contained in the spannable
        selectionStartPosition = if (selectionStartPosition < 0) 0 else selectionStartPosition
        selectionEndPosition = if (selectionEndPosition >= spannable.length) spannable.length - 1 else selectionEndPosition

        return LinkedList(Arrays.asList(*spannable.getSpans(selectionStartPosition, selectionEndPosition, ListSpan::class.java)))
    }

    interface MarkupControlsCallbacks {
        fun boldToggled(on: Boolean)
        fun italicToggled(on: Boolean)
        fun orderedListToggled(on: Boolean)
        fun unOrderedListToggled(on: Boolean)
    }
}

package com.devbrackets.android.androidmarkup.parser.core

import android.graphics.Typeface
import android.text.Spannable
import android.text.Spanned
import android.text.style.StyleSpan
import com.devbrackets.android.androidmarkup.text.style.ListSpan
import java.util.*

/**
 * An abstract base class that supports the Markup editing in the
 * [com.devbrackets.android.androidmarkup.widget.MarkupEditText]
 *
 *
 * For the simplification of examples the pipe character "|" will represent selection points,
 * the underscore character "_" will represent the current span, and a the asterisk character "*"
 * will represent any characters between the span endpoints and the selection points.
 */
abstract class MarkupParser {
    /**
     * Converts the specified markup text in to a Spanned
     * for use in the [com.devbrackets.android.androidmarkup.widget.MarkupEditText]
     *
     * @param text The markup text to convert to a spanned
     * @return The resulting spanned
     */
    abstract fun toSpanned(text: String): Spanned

    /**
     * Converts the specified spanned in to the corresponding markup.  The outputs from
     * this and [.toSpanned] should be interchangeable.
     *
     * @param spanned The Spanned to convert to markup
     * @return The markup representing the Spanned
     */
    abstract fun fromSpanned(spanned: Spanned): String

    open fun updateSpan(spannable: Spannable, spanType: Int, startIndex: Int, endIndex: Int): Boolean {
        when (spanType) {
            SpanType.BOLD -> {
                style(spannable, startIndex, endIndex, Typeface.BOLD)
                return true
            }

            SpanType.ITALIC -> {
                style(spannable, startIndex, endIndex, Typeface.ITALIC)
                return true
            }

            SpanType.ORDERED_LIST -> {
                list(spannable, startIndex, endIndex, true)
                return true
            }

            SpanType.UNORDERED_LIST -> {
                list(spannable, startIndex, endIndex, false)
                return true
            }
        }

        return false
    }

    protected fun style(spannable: Spannable, selectionStart: Int, selectionEnd: Int, style: Int) {
        val overlappingSpans = getOverlappingStyleSpans(spannable, selectionStart, selectionEnd, style)
        val shouldStyleFullWord = shouldStyleFullWord(spannable, selectionStart, selectionEnd, style)

        var modifiedSpan = false
        for (span in overlappingSpans) {
            val spanStart = spannable.getSpanStart(span)
            val spanEnd = spannable.getSpanEnd(span)

            if (spanStart == selectionStart && spanEnd == selectionEnd) {
                modifiedSpan = true
                spannable.removeSpan(span)
                continue
            } else if (shouldStyleFullWord) {
                modifiedSpan = true
                updateFullWordStyle(spannable, selectionStart, selectionEnd, span)
                continue
            }

            modifiedSpan = modifiedSpan or handleSpanStartBeforeSelection(spannable, span, spanStart, spanEnd, selectionStart, selectionEnd)
            modifiedSpan = modifiedSpan or handleSpanStartAfterSelection(spannable, span, spanStart, spanEnd, selectionStart, selectionEnd)
        }

        if (!modifiedSpan) {
            if (shouldStyleFullWord) {
                styleFullWord(selectionEnd, selectionStart, spannable, style)
            } else {
                spannable.setSpan(StyleSpan(style), selectionStart, selectionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        optimizeSpans(spannable, getOverlappingStyleSpans(spannable, selectionStart - 1, selectionEnd + 1, style))
    }

    protected fun styleFullWord(selectionEnd: Int, selectionStart: Int, spannable: Spannable, style: Int) {
        var previousWhitespace = findPreviousWhitespaceOrStart(spannable, selectionStart)
        val nextWhitespace = findNextWhitespaceOrEnd(spannable, selectionEnd)

        // If the previous and next whitespace indexes are the same we are at the end of a word
        // Find the whitespace before the word instead
        if (previousWhitespace == nextWhitespace) {
            previousWhitespace = findPreviousWhitespaceOrStart(spannable, selectionStart - 1)
        }

        if (previousWhitespace > 0) previousWhitespace++; // If the span isn't at the beginning do not include the whitespace in the span
        spannable.setSpan(StyleSpan(style), previousWhitespace,
                nextWhitespace, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    protected  fun updateFullWordStyle(spannable: Spannable, selectionStart: Int, selectionEnd: Int, span: StyleSpan) {
        val spanStart = spannable.getSpanStart(span)
        val spanEnd = spannable.getSpanEnd(span)
        val previousWhitespace = findPreviousWhitespaceOrStart(spannable, selectionStart)
        val nextWhitespace = findNextWhitespaceOrEnd(spannable, selectionEnd)

        if (spanStart < previousWhitespace) {
            spannable.setSpan(StyleSpan(span.style), spanStart, previousWhitespace, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        if (spanEnd > nextWhitespace) {
            spannable.setSpan(StyleSpan(span.style), nextWhitespace + 1, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        spannable.removeSpan(span)
    }

    protected fun list(spannable: Spannable, selectionStart: Int, selectionEnd: Int, ordered: Boolean) {
        var selectionStart = selectionStart
        var selectionEnd = selectionEnd
        val overlappingSpans = getOverlappingListSpans(spannable, selectionStart, selectionEnd)

        //Updates the selectionStart to the new line
        if (selectionStart != 0) {
            val previousNewline = findPreviousChar(spannable, selectionStart, '\n')
            selectionStart = if (previousNewline == -1) 0 else previousNewline
        }

        //Updates the selectionEnd to the new line
        if (selectionEnd != spannable.length - 1) {
            val nextNewline = findNextChar(spannable, selectionEnd, '\n')
            selectionEnd = if (nextNewline == -1) spannable.length - 1 else nextNewline
        }

        var modifiedSpan = false
        for (span in overlappingSpans) {
            val spanStart = spannable.getSpanStart(span)
            val spanEnd = spannable.getSpanEnd(span)

            if (spanStart == selectionStart && spanEnd == selectionEnd) {
                modifiedSpan = true
                spannable.removeSpan(span)
                continue
            }

            modifiedSpan = modifiedSpan or handleSpanStartBeforeSelection(spannable, span, spanStart, spanEnd, selectionStart, selectionEnd)
            modifiedSpan = modifiedSpan or handleSpanStartAfterSelection(spannable, span, spanStart, spanEnd, selectionStart, selectionEnd)
        }

        if (!modifiedSpan) {
            spannable.setSpan(ListSpan(if (ordered) ListSpan.Type.NUMERICAL else ListSpan.Type.BULLET), selectionStart, selectionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        optimizeSpans(spannable, getOverlappingListSpans(spannable, selectionStart - 1, selectionEnd + 1))
    }

    /**
     * If the specified Span starts before or equal to the selection, then we need to update the span end
     * only if the span ending is less than the `selectionEnd`.  If the span ending is
     * greater than or equal to the `selectionEnd` then the selected area will have the style
     * removed.
     *
     *
     * The cases that need to be handled below are:
     *
     *  1.
     * The selection start is contained within or equal to the span start and the selection end goes beyond the
     * span end.  (e.g. __|___***| will result in __|______| or |___***| will result in |______|)
     *
     *  1.
     * The selection start is equal to the span start and the span end is contained within the
     * span.  (e.g. |______|__ will result in |******|__)
     *
     *  1.
     * Both the selection start and end are contained within the span.
     * (e.g. __|______|__ will result in __|******|__)
     *
     *  1.
     * The selection start is contained within the span and the selection end is equal to the
     * span end.  (e.g. __|______| will result in __|******|)
     */
    protected fun handleSpanStartBeforeSelection(spannable: Spannable, span: Any, spanStart: Int, spanEnd: Int, selectionStart: Int, selectionEnd: Int): Boolean {
        if (spanStart > selectionStart) {
            //handled by handleSpanStartAfterSelection
            return false
        }

        //Handles the first case listed above
        if (spanEnd < selectionEnd) {
            spannable.setSpan(span, spanStart, selectionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }

        //Handles the second case listed above
        if (selectionStart == spanStart && spanEnd > selectionEnd) {
            spannable.setSpan(span, selectionEnd, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }

        //Handles the third case listed above
        if (spanEnd > selectionEnd) {
            spannable.setSpan(span, spanStart, selectionStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            val duplicate = duplicateSpan(span)
            if (duplicate != null) {
                spannable.setSpan(duplicate, selectionEnd, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            return true
        }

        //Handles the final case listed above
        spannable.setSpan(span, spanStart, selectionStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return true
    }

    /**
     * If the specified Span starts after the `selectionStart`, then we need to update the span start
     * to the selection.  Additionally, if the Span ends before the `selectionEnd`, we need to
     * update the span end as well.
     *
     *
     * The cases that need to be handled below are:
     *
     *  1.
     * The selection start is before the `spanStart` and the `selectionEnd`
     * is after the span end. (e.g. |***___***| will result in |_________|)
     *
     *  1.
     * The selection start is before the `spanStart` and the `selectionEnd`
     * is before or equal to the span end. (e.g. (|***___| will result in |______| or |***___|___
     * will result in |______|___)
     */
    protected fun handleSpanStartAfterSelection(spannable: Spannable, span: Any, spanStart: Int, spanEnd: Int, selectionStart: Int, selectionEnd: Int): Boolean {
        if (spanStart <= selectionStart) {
            //handled by handleSpanStartBeforeSelection
            return false
        }

        //Handles the first case listed above
        if (spanEnd < selectionEnd) {
            spannable.setSpan(span, selectionStart, selectionEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }

        //Handles the final case listed above
        spannable.setSpan(span, selectionStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return true
    }

    protected fun getOverlappingStyleSpans(spannable: Spannable, selectionStart: Int, selectionEnd: Int, style: Int): List<StyleSpan> {
        var selectionStart = selectionStart
        var selectionEnd = selectionEnd
        //Makes sure the start and end are contained in the spannable
        selectionStart = if (selectionStart < 0) 0 else selectionStart
        selectionEnd = if (selectionEnd >= spannable.length) spannable.length - 1 else selectionEnd

        val spans = LinkedList(Arrays.asList(*spannable.getSpans(selectionStart, selectionEnd, StyleSpan::class.java)))

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
        var selectionStart = selectionStart
        var selectionEnd = selectionEnd
        //Makes sure the start and end are contained in the spannable
        selectionStart = if (selectionStart < 0) 0 else selectionStart
        selectionEnd = if (selectionEnd >= spannable.length) spannable.length - 1 else selectionEnd

        return LinkedList(Arrays.asList(*spannable.getSpans(selectionStart, selectionEnd, ListSpan::class.java)))
    }

    /**
     * Optimizes the spans by joining any overlapping or abutting spans of
     * the same type.  This assumes that the specified `spans`
     * are of the same type.
     *
     * NOTE: this method is O(n^2) for `spans`
     *
     * @param spannable The spannable that the `spans` are associated with
     *
     * @param spans     The spans to optimize
     */
    protected fun optimizeSpans(spannable: Spannable, spans: List<*>) {
        val removeSpans = HashSet<Any>()

        for (span in spans) {
            if (removeSpans.contains(span)) {
                continue
            }

            for (compareSpan in spans) {
                if (span !== compareSpan && !removeSpans.contains(compareSpan) && compareAndMerge(spannable, span!!, compareSpan!!)) {
                    removeSpans.add(compareSpan)
                }
            }
        }

        // Actually remove any spans that were merged (the compareSpan items)
        for (span in removeSpans) {
            spannable.removeSpan(span)
        }
    }

    /**

     * @param spannable The spannable that the spans to check for overlaps are associated with
     *
     * @param lhs The first span object to determine if it overlaps with `rhs`.
     *            If the spans are merged, this will be the span left associated with the
     *            `spannable`
     *
     * @param rhs The second span object to determine if it overlaps with `lhs`.
     *            If the spans are merged, this will be the span removed from the
     *            `spannable`
     *
     * @return True if the spans have been merged
     */
    protected fun compareAndMerge(spannable: Spannable, lhs: Any, rhs: Any): Boolean {
        val lhsStart = spannable.getSpanStart(lhs)
        val lhsEnd = spannable.getSpanEnd(lhs)
        val rhsStart = spannable.getSpanStart(rhs)
        val rhsEnd = spannable.getSpanEnd(rhs)

        if (lhsStart < rhsStart && rhsStart <= lhsEnd) {
            val end = if (lhsEnd > rhsEnd) lhsEnd else rhsEnd
            spannable.setSpan(lhs, lhsStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        } else if (lhsStart >= rhsStart && lhsStart <= rhsEnd) {
            val end = if (lhsEnd > rhsEnd) lhsEnd else rhsEnd
            spannable.setSpan(lhs, rhsStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return true
        }

        return false
    }

    /**
     * Used to duplicate spans when splitting an existing span in to two.
     * This would occur when the selection is only a partial of the styled
     * text and the styling is removed.
     *
     * @param span The span to duplicate
     * @return The duplicate span or null
     */
    protected fun duplicateSpan(span: Any): Any? {
        if (span is StyleSpan) {
            return StyleSpan(span.style)
        }

        return null
    }

    protected fun findPreviousChar(spannable: Spannable, start: Int, character: Char): Int {
        var start = start
        if (start < 0) {
            return -1
        }

        if (start >= spannable.length) {
            start = spannable.length - 1
        }

        for (i in start downTo 0) {
            if (spannable[i] == character) {
                return i
            }
        }

        return -1
    }

    protected fun findNextChar(spannable: Spannable, start: Int, character: Char): Int {
        var start = start
        if (start < 0) {
            start = 0
        }

        if (start >= spannable.length) {
            return -1
        }

        for (i in start..spannable.length - 1) {
            if (spannable[i] == character) {
                return i
            }
        }

        return -1
    }

    /**
     * Finds the index of the previous space or new line character or 0 if the
     * there are no instances between the startIndex and the beginning of the spannable
     */
    protected fun findPreviousWhitespaceOrStart(spannable: Spannable, startIndex: Int) : Int {
        var start = startIndex
        if (start < 0) {
            return -1
        }

        if (start >= spannable.length) {
            start = spannable.length - 1
        }

        for (i in start downTo 0) {
            val spanCharacter = spannable[i]
            if (spanCharacter == '\n' || spanCharacter == ' ') {
                return i
            }
        }

        return 0
    }

    /**
     * Finds the index of the next space or new line character or the last index of the spannable if
     * there are no instances between the startIndex and the end of the spannable
     */
    protected fun findNextWhitespaceOrEnd(spannable: Spannable, startIndex: Int): Int {
        var start = startIndex
        if (start < 0) {
            start = 0
        }

        if (start >= spannable.length) {
            return -1
        }

        for (i in start..spannable.length - 1) {
            val spanCharacter = spannable[i]
            if (spanCharacter == '\n' || spanCharacter == ' ') {
                return i
            }
        }

        return spannable.length
    }

    protected fun shouldStyleFullWord(spannable: Spannable, selectionStart: Int, selectionEnd: Int, style: Int) : Boolean {
        val shouldStyle = selectionStart == selectionEnd && (style == Typeface.BOLD || style == Typeface.ITALIC)
        val isOnAWord = spannable[selectionStart].isLetterOrDigit() || spannable[selectionStart-1].isLetterOrDigit()
        return if (selectionStart >= 0 && selectionStart < spannable.length && isOnAWord) shouldStyle else false
    }
}

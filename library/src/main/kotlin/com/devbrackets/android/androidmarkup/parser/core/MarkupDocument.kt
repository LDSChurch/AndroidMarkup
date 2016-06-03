package com.devbrackets.android.androidmarkup.parser.core

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import com.devbrackets.android.androidmarkup.text.style.ListSpan
import java.util.*

open class MarkupDocument() {
    protected val rootElement: MarkupElement = MarkupElement(null)

    constructor(spanned: Spanned) : this() {
        val spans = findRelevantSpans(spanned, IndexSpanComparator(spanned))
        parseSpanned(spanned, spans, 0, spanned.length -1, rootElement)
    }

    open fun toSpanned() : Spanned {
        val spanned = SpannableStringBuilder("")
        toSpanned(spanned, rootElement)

        return spanned
    }

    open protected fun toSpanned(builder: SpannableStringBuilder, parent: MarkupElement) {
        val startIndex = builder.length
        parent.text?.let {
            builder.append(it)
        }

        for (element in parent.children) {
            toSpanned(builder, element)
        }

        val endIndex = builder.length
        val spanObj = getSpanObject(parent.spanType)

        spanObj?.let {
            builder.setSpan(it, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    //Inclusive indexes, parse moving from start to end (e.g. 0 to 100)
    open protected fun parseSpanned(spanned: Spanned, spans: List<Any>, startIndex: Int, endIndex: Int, parent: MarkupElement) : Int {
        var workingTextElement : MarkupElement? = null

        //Iterate through the range looking for collisions
        var index = startIndex -1
        while (++index <= endIndex) {
            //Finds any spans that contain the current index, if none exists append the character at the index
            var collisionSpans = findSpansForIndex(index, spanned, spans)
            if (collisionSpans.isEmpty()) {
                if (workingTextElement == null) {
                    workingTextElement = MarkupElement(parent)
                    workingTextElement.spanType = SpanType.TEXT
                    parent.addChild(workingTextElement)
                }

                workingTextElement.text = "${workingTextElement.text.orEmpty()}${spanned[index]}"
                continue
            }

            //If there are multiple span collisions, find the containing span and use that as the 'currentSpan' argument
            val containingSpan = findContainingSpan(spanned, collisionSpans)
            collisionSpans = findSpansForRange(spanned.getSpanStart(containingSpan), spanned.getSpanEnd(containingSpan), spanned, spans)
            collisionSpans.remove(containingSpan)
            workingTextElement = null

            val spanElement = MarkupElement(parent)
            spanElement.spanType = getSpanType(containingSpan)
            parent.addChild(spanElement)

            index = parseSpanned(spanned, collisionSpans, index, spanned.getSpanEnd(containingSpan) -1, spanElement)
        }

        //Subtracts 1 due to the index being incremented before the comparison
        return index -1
    }

    /**
     * Finds the span that contains all other spans in the `spans`
     */
    open protected fun findContainingSpan(spanned: Spanned, spans: List<Any>) : Any {
        var currentContainer = spans[0]
        var currentSpanStart = spanned.getSpanStart(currentContainer)
        var currentSpanEnd = spanned.getSpanEnd(currentContainer)

        for (i in 1 .. spans.size -1) {
            if (spanned.getSpanStart(spans[i]) <= currentSpanStart && spanned.getSpanEnd(spans[i]) > currentSpanEnd) {
                currentContainer = spans[i]
                currentSpanStart = spanned.getSpanStart(currentContainer)
                currentSpanEnd = spanned.getSpanEnd(currentContainer)
            }
        }

        return currentContainer
    }

    open protected fun findSpansForIndex(index: Int, spanned: Spanned, spans: List<Any>) : MutableList<Any> {
        val collisionSpans = mutableListOf<Any>()
        for (span in spans) {
            if (spanned.getSpanStart(span) <= index && spanned.getSpanEnd(span)-1 >= index) {
                collisionSpans.add(span)
            }
        }

        return collisionSpans
    }

    open protected fun findSpansForRange(startIndex: Int, endIndex: Int, spanned: Spanned, spans: List<Any>) : MutableList<Any> {
        val collisionSpans = mutableListOf<Any>()
        for (span in spans) {
            if (spanned.getSpanStart(span) >= startIndex && spanned.getSpanEnd(span) <= endIndex) {
                collisionSpans.add(span)
            }
        }

        return collisionSpans
    }

    /**
     * Finds all spans that are used for Markup processing, filtering out
     * unused spans such as those used by the EditText for marking selection
     */
    open protected fun findRelevantSpans(spanned: Spanned, comparator: Comparator<Any>): List<Any> {
        val spans = LinkedList(Arrays.asList(*spanned.getSpans(0, spanned.length, Any::class.java)))
        if (spans.isEmpty()) {
            return spans
        }

        //remove irrelevant span types
        val iterator = spans.iterator()
        while (iterator.hasNext()) {
            if (!supportedSpan(iterator.next())) {
                iterator.remove()
            }
        }

        Collections.sort(spans, comparator)
        return spans
    }

    open protected fun supportedSpan(span: Any): Boolean {
        when (span) {
            is StyleSpan -> return true
            is ListSpan -> return true
            else -> return false
        }
    }

    open protected fun getSpanType(span: Any): Int {
        when (span) {
            is StyleSpan -> return if (span.style == Typeface.BOLD) SpanType.BOLD else SpanType.ITALIC
            is ListSpan -> return if (span.type == ListSpan.Type.BULLET) SpanType.UNORDERED_LIST else SpanType.ORDERED_LIST
            else -> return SpanType.TEXT
        }
    }

    open protected fun getSpanObject(spanType: Int) : Any? {
        when (spanType) {
            SpanType.BOLD -> return StyleSpan(Typeface.BOLD)
            SpanType.ITALIC -> return StyleSpan(Typeface.ITALIC)
            SpanType.ORDERED_LIST -> return ListSpan(ListSpan.Type.NUMERICAL)
            SpanType.UNORDERED_LIST -> return ListSpan(ListSpan.Type.BULLET)
            else -> return null
        }
    }

    /**
     * Used to sort the spans so that the span the ends last (greatest value)
     * will be listed first
     */
    open protected class IndexSpanComparator(private val spanned: Spanned) : Comparator<Any> {
        override fun compare(lhs: Any, rhs: Any): Int {
            return spanned.getSpanEnd(lhs) - spanned.getSpanEnd(rhs)
        }
    }
}
package com.devbrackets.android.androidmarkup.text.style

class ListSpan @JvmOverloads constructor(val type: ListSpan.Type = ListSpan.Type.BULLET) {

    enum class Type {
        BULLET,
        NUMERICAL
    }
}
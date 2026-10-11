package com.example.comp90018.permissions

/** Keeps a request alive until completion; clears it before calling reentrant business code. */
internal class PendingRequest<T, R> {
    var type: T? = null
        private set
    private var callback: ((R) -> Unit)? = null

    fun begin(type: T, callback: (R) -> Unit): Boolean {
        if (this.type != null) return false
        this.type = type
        this.callback = callback
        return true
    }

    fun complete(result: R) {
        val completion = callback
        type = null
        callback = null
        completion?.invoke(result)
    }
}

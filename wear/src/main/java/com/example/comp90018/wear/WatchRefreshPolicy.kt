package com.example.comp90018.wear

/** Local demo ticks smoothly without increasing real phone Data Layer polling. */
internal fun watchRefreshIntervalMillis(isDemo: Boolean): Long =
    if (isDemo) 1_000L else 3_000L

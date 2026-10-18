package com.beatbridge

internal object UpdateNotice {
    const val COOLDOWN_MS = 14L * 24 * 60 * 60 * 1000

    fun shouldShow(previousBuild: Long?, currentBuild: Long, lastShownMs: Long?, nowMs: Long): Boolean =
        previousBuild != null &&
            currentBuild > previousBuild &&
            (lastShownMs == null || nowMs - lastShownMs >= COOLDOWN_MS)
}

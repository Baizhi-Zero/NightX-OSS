package net.baizhi.client.features.api

import net.baizhi.client.event.EventTarget
import net.baizhi.client.event.Listenable
import net.baizhi.client.event.Render2DEvent
import net.baizhi.client.event.TickEvent
import net.baizhi.client.utils.ClientUtils
import java.util.Locale

class PerformanceMonitor : Listenable {

    private var frameCount = 0
    private var windowStartNanos = 0L
    private var lastFrameNanos = 0L
    private var maxFrameMs = 0.0

    private var lastTickNanos = 0L
    private var maxTickGapMs = 0.0

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val now = System.nanoTime()

        if (lastFrameNanos != 0L) {
            val frameMs = (now - lastFrameNanos) / 1_000_000.0
            if (frameMs > maxFrameMs) maxFrameMs = frameMs
        }
        lastFrameNanos = now
        frameCount++

        if (windowStartNanos == 0L) {
            windowStartNanos = now
            return
        }

        if (now - windowStartNanos >= 3_000_000_000L) {
            val fps = frameCount * 1_000_000_000.0 / (now - windowStartNanos)
            val fpsText = String.format(Locale.US, "%.1f", fps)
            val frameText = String.format(Locale.US, "%.1f", maxFrameMs)
            val tickText = String.format(Locale.US, "%.1f", maxTickGapMs)

            ClientUtils.getLogger().info(
                "[PERF] FPS=$fpsText MaxFrame=${frameText}ms MaxTickGap=${tickText}ms (3s window)"
            )

            frameCount = 0
            maxFrameMs = 0.0
            maxTickGapMs = 0.0
            windowStartNanos = now
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        val now = System.nanoTime()
        if (lastTickNanos != 0L) {
            val gapMs = (now - lastTickNanos) / 1_000_000.0
            if (gapMs > maxTickGapMs) maxTickGapMs = gapMs
        }
        lastTickNanos = now
    }

    override fun handleEvents(): Boolean = true
}

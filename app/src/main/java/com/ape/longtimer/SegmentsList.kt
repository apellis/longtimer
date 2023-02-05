package com.ape.longtimer

import com.ape.longtimer.model.Segment

class SegmentsList(private val segments: MutableList<Segment>) {
    private val bellTimesSecs: MutableList<Int> = mutableListOf()

    init {
        updateBellTimes()
    }

    fun getAll(): List<Segment> {
        return segments
    }

    fun get(index: Int): Segment {
        return segments[index]
    }

    fun add(index: Int, segment: Segment) {
        segments.add(index, segment)
        updateBellTimes()
    }

    fun add(segment: Segment) {
        segments.add(segment)
        updateBellTimes()
    }

    fun getSize(): Int {
        return segments.size
    }

    fun getActiveSegmentIndex(atSecs: Int): Int {
        val pos = bellTimesSecs.binarySearch(atSecs)
        return if (pos >= 0) {
            // The (i+1)th segment starts at the ith bell time (0 is not a bell time)
            pos + 1
        } else {
            -pos - 1
        }
    }

    fun shouldRingAt(secs: Int): Boolean {
        return bellTimesSecs.binarySearch(secs) >= 0
    }

    fun secsUntilNextBell(fromSecs: Int): Int {
        val pos = bellTimesSecs.binarySearch(fromSecs)
        return if (pos >= 0) {
            0  // we're at a bell time
        } else {
            val nextTime = bellTimesSecs[-pos - 1]
            nextTime - fromSecs
        }
    }

    fun getTotalDurationSecs(): Int {
        var ret = 0
        for (segment in segments) ret += segment.durationSecs
        return ret
    }

    private fun updateBellTimes() {
        bellTimesSecs.clear()
        var lastTime: Int = 0
        for (segment in segments) {
            lastTime += segment.durationSecs
            bellTimesSecs.add(lastTime)
        }
    }
}
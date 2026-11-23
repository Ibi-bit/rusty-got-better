package dev.rusty.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemVolumeTest {
    @Test
    fun mapsStreamIndexOfAFifteenStepDevice() {
        assertEquals(0, SystemVolume.toPercent(0, 15))
        assertEquals(47, SystemVolume.toPercent(7, 15))
        assertEquals(100, SystemVolume.toPercent(15, 15))
        assertEquals(7, SystemVolume.toIndex(47, 15))
    }

    @Test
    fun everyIndexRoundTripsOnCommonStepCounts() {
        for (max in listOf(15, 25, 30, 100)) {
            for (index in 0..max) {
                assertEquals(index, SystemVolume.toIndex(SystemVolume.toPercent(index, max)!!, max))
            }
        }
    }

    @Test
    fun clampsOutOfRangePercent() {
        assertEquals(0, SystemVolume.toIndex(-20, 15))
        assertEquals(15, SystemVolume.toIndex(400, 15))
        assertEquals(0, SystemVolume.toIndex(50, 0))
    }

    @Test
    fun rejectsABogusMaximum() {
        assertNull(SystemVolume.toPercent(3, 0))
        assertNull(SystemVolume.toPercent(3, -1))
    }

    @Test
    fun keepsANonZeroPercentAudible() {
        assertEquals(0, SystemVolume.toIndex(0, 15))
        assertEquals(1, SystemVolume.toIndex(1, 15))
        assertEquals(1, SystemVolume.toIndex(6, 15))
        assertEquals(15, SystemVolume.toIndex(100, 15))
    }

    @Test
    fun tapersTheBottomOfAFifteenStepStream() {
        assertEquals(0.15f, SystemVolume.outputGain(1, 15), 0.001f)
        assertEquals(0.4f, SystemVolume.outputGain(2, 15), 0.001f)
        assertEquals(0.7f, SystemVolume.outputGain(3, 15), 0.001f)
        assertEquals(1f, SystemVolume.outputGain(4, 15), 0.001f)
        assertEquals(1f, SystemVolume.outputGain(15, 15), 0.001f)
    }

    @Test
    fun leavesASingleStepOrBogusStreamAlone() {
        assertEquals(1f, SystemVolume.outputGain(1, 1), 0.001f)
        assertEquals(1f, SystemVolume.outputGain(1, 0), 0.001f)
    }
}

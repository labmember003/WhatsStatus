package com.geeksoftapps.whatsweb.status

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusScannerTest {

    @Test
    fun mimeTypeWins() {
        assertEquals(StatusType.IMAGE, statusTypeOf("image/jpeg", "a.bin"))
        assertEquals(StatusType.VIDEO, statusTypeOf("video/mp4", "a"))
    }

    @Test
    fun extensionFallbackWhenProviderHasNoType() {
        assertEquals(StatusType.IMAGE, statusTypeOf(null, "IMG-20261128-WA0001.JPG"))
        assertEquals(StatusType.IMAGE, statusTypeOf("application/octet-stream", "x.webp"))
        assertEquals(StatusType.VIDEO, statusTypeOf("application/octet-stream", "VID-1.mp4"))
        assertEquals(StatusType.OTHER, statusTypeOf(null, "voice.opus"))
        assertEquals(StatusType.OTHER, statusTypeOf(null, "noextension"))
        assertEquals(StatusType.OTHER, statusTypeOf(null, null))
    }

    @Test
    fun skipsHiddenTempAndEmptyFiles() {
        assertFalse(StatusScanner.isStatusMedia(".nomedia", null, 0))
        assertFalse(StatusScanner.isStatusMedia(".hidden.jpg", "image/jpeg", 10))
        assertFalse(StatusScanner.isStatusMedia("a.jpg", "image/jpeg", 0))
        assertFalse(StatusScanner.isStatusMedia("a.mp4.tmp", null, 10))
        assertFalse(StatusScanner.isStatusMedia(null, "image/jpeg", 10))
        assertTrue(StatusScanner.isStatusMedia("a.jpg", "image/jpeg", 10))
        assertTrue(StatusScanner.isStatusMedia("b.mp4", null, 10))
    }
}

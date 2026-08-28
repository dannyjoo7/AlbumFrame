package kr.joolabs.albumframe.domain

import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PhotoPlaylistTest {
    @Test
    fun chronologicalOrderSupportsNextAndPrevious() {
        val playlist = PhotoPlaylist(
            listOf("a", "b", "c"),
            SlideshowOrder.CHRONOLOGICAL,
        )

        assertEquals("a", playlist.current)
        assertEquals("b", playlist.next())
        assertEquals("a", playlist.previous())
        assertEquals("c", playlist.previous())
    }

    @Test
    fun shuffledCycleVisitsEveryItemOnce() {
        val playlist = PhotoPlaylist(
            sourceItems = listOf("a", "b", "c"),
            initialOrder = SlideshowOrder.SHUFFLED,
            shuffle = { it.reverse() },
        )

        val cycle = listOf(playlist.current, playlist.next(), playlist.next())

        assertEquals(setOf("a", "b", "c"), cycle.toSet())
    }

    @Test
    fun shuffledBoundaryNeverRepeatsPreviousPhoto() {
        var shuffleCount = 0
        val playlist = PhotoPlaylist(
            sourceItems = listOf("a", "b", "c"),
            initialOrder = SlideshowOrder.SHUFFLED,
            shuffle = {
                shuffleCount++
                if (shuffleCount > 1) Collections.rotate(it, 1)
            },
        )
        playlist.next()
        val previous = playlist.next()

        val next = playlist.next()

        assertEquals("c", previous)
        assertNotEquals(previous, next)
    }

    @Test
    fun changingOrderKeepsCurrentPhoto() {
        val playlist = PhotoPlaylist(
            sourceItems = listOf("a", "b", "c"),
            initialOrder = SlideshowOrder.CHRONOLOGICAL,
            shuffle = { it.reverse() },
        )
        playlist.next()

        playlist.changeOrder(SlideshowOrder.SHUFFLED)

        assertEquals("b", playlist.current)
    }

    @Test
    fun emptyPlaylistIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoPlaylist(emptyList<String>(), SlideshowOrder.CHRONOLOGICAL)
        }
    }
}

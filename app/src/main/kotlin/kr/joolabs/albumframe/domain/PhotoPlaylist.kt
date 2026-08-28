package kr.joolabs.albumframe.domain

import java.util.Collections

/** 앱과 시스템 화면보호기가 함께 사용하는 사진 재생 순서 정책이다. */
class PhotoPlaylist<T>(
    sourceItems: List<T>,
    initialOrder: SlideshowOrder,
    private val shuffle: (MutableList<T>) -> Unit = { it.shuffle() },
) {
    private val source = sourceItems.toList()
    private var items = source.toMutableList()
    private var index = 0

    var order: SlideshowOrder = initialOrder
        private set

    init {
        require(items.isNotEmpty()) { "A photo playlist needs at least one item" }
        if (order == SlideshowOrder.SHUFFLED) shuffle(items)
    }

    val size: Int
        get() = items.size

    val position: Int
        get() = index

    val current: T
        get() = items[index]

    fun next(): T {
        val previous = current
        index++
        if (index < items.size) return current

        index = 0
        if (order == SlideshowOrder.SHUFFLED && items.size > 1) {
            shuffle(items)
            if (items.first() == previous) {
                val replacement = (1 until items.size)
                    .firstOrNull { items[it] != previous }
                    ?: 1
                Collections.swap(items, 0, replacement)
            }
        }
        return current
    }

    fun previous(): T {
        index = if (index == 0) items.lastIndex else index - 1
        return current
    }

    fun changeOrder(nextOrder: SlideshowOrder) {
        if (order == nextOrder) return
        val active = current
        order = nextOrder
        items = source.toMutableList()
        if (order == SlideshowOrder.SHUFFLED) shuffle(items)
        index = items.indexOf(active).coerceAtLeast(0)
    }
}

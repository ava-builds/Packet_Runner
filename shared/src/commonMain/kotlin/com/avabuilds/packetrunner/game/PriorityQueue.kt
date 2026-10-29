package com.avabuilds.packetrunner.game

/**
 * A priority queue (binary min-heap): [poll] gives the smallest element by [comparator]. It replaces java.util.PriorityQueue,
 * which does not exist on every platform, and has the same small API the pathfinding needs.
 */
internal class PriorityQueue<T>(private val comparator: Comparator<in T>) {
    private val items = ArrayList<T>()

    fun isNotEmpty(): Boolean = items.isNotEmpty()

    fun add(item: T) {
        items.add(item)
        var i = items.lastIndex
        while (i > 0) {
            val parent = (i - 1) / 2
            if (comparator.compare(items[i], items[parent]) >= 0) break
            swap(i, parent)
            i = parent
        }
    }

    /** Removes and returns the smallest element, or null when the queue is empty. */
    fun poll(): T? {
        if (items.isEmpty()) return null
        val top = items[0]
        val last = items.removeAt(items.lastIndex)
        if (items.isNotEmpty()) {
            items[0] = last
            var i = 0
            while (true) {
                val left = 2 * i + 1
                val right = left + 1
                var smallest = i
                if (left < items.size && comparator.compare(items[left], items[smallest]) < 0) smallest = left
                if (right < items.size && comparator.compare(items[right], items[smallest]) < 0) smallest = right
                if (smallest == i) break
                swap(i, smallest)
                i = smallest
            }
        }
        return top
    }

    private fun swap(a: Int, b: Int) {
        val t = items[a]; items[a] = items[b]; items[b] = t
    }
}

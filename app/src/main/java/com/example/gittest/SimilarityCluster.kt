package com.example.gittest.ml

class SimilarityCluster(
    private val data: List<FloatArray>,
    private val eps: Float = 10f,
    private val minPts: Int = 2
) {
    private val visited = BooleanArray(data.size)
    private val labels = IntArray(data.size) { -1 }

    fun cluster(): IntArray {
        var clusterId = 0
        for (i in data.indices) {
            if (visited[i]) continue
            visited[i] = true

            val neighbors = regionQuery(i)
            if (neighbors.size < minPts) {
                labels[i] = -1
            } else {
                expandCluster(i, neighbors, clusterId++)
            }
        }
        return labels
    }

    private fun expandCluster(pointIdx: Int, neighbors: List<Int>, clusterId: Int) {
        labels[pointIdx] = clusterId
        val queue = neighbors.toMutableList()

        while (queue.isNotEmpty()) {
            val idx = queue.removeAt(0)
            if (!visited[idx]) {
                visited[idx] = true
                val n = regionQuery(idx)
                if (n.size >= minPts) {
                    queue.addAll(n)
                }
            }
            if (labels[idx] == -1) {
                labels[idx] = clusterId
            }
        }
    }

    private fun regionQuery(idx: Int): List<Int> {
        val neighbors = mutableListOf<Int>()
        for (j in data.indices) {
            val dist = l2Distance(data[idx], data[j])
            if (dist < eps) neighbors.add(j)
        }
        return neighbors
    }

    private fun l2Distance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return kotlin.math.sqrt(sum)
    }
}

/**
 * Data class to hold the result of a Tabu Search:
 *  - [bestTour]      : The best tour found (list of node indices).
 *  - [bestDistance]  : The distance of that best tour.
 *  - [bestIter]      : The iteration at which the best tour was found.
 *  - [evaluated]     : Count of evaluated moves.
 */
data class TabuResult(
    val bestTour: List<Int>,
    val bestDistance: Float,
    val bestIter: Int,
    val evaluated: Int
)

/**
 * TabuSearch class for TSP.
 *
 * This class holds all data for performing a Tabu Search on a TSP instance.
 *
 * @property distanceMatrix The TSP distance matrix.
 * @property n              The number of nodes.
 * @property tabuList       2D list tracking how many iterations a particular edge is "tabu."
 * @property tabuTenure     Number of iterations an edge swap remains tabu.
 * @property tabuEliteMoves List of potential 2-opt moves (i, j, delta) that are "elite."
 * @property maxIter        Maximum number of iterations to try without improvement.
 * @property maxMoves       Max moves to keep in the "elite" set each iteration.
 */
class TabuSearch(
    val distanceMatrix: List<List<Float>>,
    val n: Int,
    var tabuList: MutableList<MutableList<Int>>,
    val tabuTenure: Int,
    var tabuEliteMoves: MutableList<Triple<Int, Int, Float>>,
    val maxIter: Int,
    val maxMoves: Int
) {
    fun run(): TabuResult {
        // Initial solution
        var currentTour = randomPermutation(n)
        var bestTour = currentTour.toList()
        var bestDistance = calculateTourDistance(bestTour, distanceMatrix)
        var currentDistance = bestDistance

        var bestIter = 0
        var iter = 0
        var evaluated = 0

        val localTabuList = tabuList.map { it.toMutableList() }.toMutableList()
        // Clear the existing 'elite' move set
        tabuEliteMoves.clear()

        // Keep looping until we've gone [maxIter] iterations without improvement
        while (iter - bestIter < maxIter) {
            iter += 1

            // If we have no stored moves from last iteration, we generate them all
            if (tabuEliteMoves.isEmpty()) {
                for (i in 0 until n) {
                    for (j in i + 1 until n) {
                        val nextI = (i + 1) % n
                        val nextJ = (j + 1) % n
                        // Skip if we wrap around in a trivial way
                        if (nextJ == i) continue

                        // Decrement tabu tenure if active
                        if (localTabuList[i][j] > 0) {
                            localTabuList[i][j] -= 1
                        }

                        val delta = getDeltaIntraRoute(
                            distanceMatrix,
                            currentTour[i], currentTour[nextI],
                            currentTour[j], currentTour[nextJ]
                        )
                        // Collect (i, j, delta)
                        tabuEliteMoves.add(Triple(i, j, delta))
                        evaluated++
                    }
                }
                // Sort by ascending delta
                tabuEliteMoves.sortBy { it.third }

                // Keep only top-k moves
                if (tabuEliteMoves.size > maxMoves) {
                    tabuEliteMoves = tabuEliteMoves.subList(0, maxMoves).toMutableList()
                }
            } else {
                // Recalculate delta for each "elite" move
                for (k in tabuEliteMoves.indices) {
                    val (i, j, _) = tabuEliteMoves[k]
                    val nextI = (i + 1) % n
                    val nextJ = (j + 1) % n
                    val newDelta = getDeltaIntraRoute(
                        distanceMatrix,
                        currentTour[i], currentTour[nextI],
                        currentTour[j], currentTour[nextJ]
                    )
                    tabuEliteMoves[k] = Triple(i, j, newDelta)
                }
                // Sort again by delta
                tabuEliteMoves.sortBy { it.third }

                // Update the tabu list
                for (i in 0 until n) {
                    for (j in i + 1 until n) {
                        if (localTabuList[i][j] > 0) {
                            localTabuList[i][j] -= 1
                        }
                    }
                }

                // If the best new move is not sufficiently improving (~-0.5%),
                if (tabuEliteMoves.isNotEmpty() &&
                    (tabuEliteMoves[0].third / currentDistance) > -0.005f
                ) {
                    tabuEliteMoves.clear()
                    continue
                }
            }

            // Pick the first non-tabu move (or one that improves the global best)
            for (i in tabuEliteMoves.indices) {
                val (moveI, moveJ, delta) = tabuEliteMoves[i]

                // If not tabu or it yields a better solution than the global best
                if (localTabuList[moveI][moveJ] == 0 ||
                    (currentDistance + delta) < bestDistance
                ) {
                    // Perform the 2-opt swap
                    val nextI = (moveI + 1) % n
                    // We'll pass bestTour.clone() or mutableListOf(...) as the last param
                    // The 'swap2Edges' returns the new route as a fresh list
                    currentTour = swap2Edges(
                        currentTour,
                        nextI,
                        moveJ,
                        bestTour.toMutableList()
                    )

                    // Update distance
                    currentDistance += delta
                    // Mark this edge pair as tabu
                    localTabuList[moveI][moveJ] = tabuTenure
                    // Remove from the "elite" list so we don't pick it again immediately
                    tabuEliteMoves.removeAt(i)
                    break
                }
            }

            // Update the global best if needed
            if (currentDistance < bestDistance) {
                bestTour = currentTour.toList()
                bestDistance = currentDistance
                bestIter = iter
            }
        }

        // Return final best results
        return TabuResult(
            bestTour = bestTour,
            bestDistance = bestDistance,
            bestIter = bestIter,
            evaluated = evaluated
        )
    }

    companion object {
        /**
         * Factory method to create a new TabuSearch instance.
         *
         * @param distanceMatrix The TSP distance matrix.
         * @param iters          Optional max iterations without improvement. Default = 100.
         */
        fun new(distanceMatrix: List<List<Float>>, iters: Int? = null): TabuSearch {
            val n = distanceMatrix.size
            val tabuTenure = n / 4
            val maxIter = iters ?: 100
            val maxMoves = n / 10

            // Build an n x n zeroed list for the tabu durations
            val initialTabuList = MutableList(n) { MutableList(n) { 0 } }

            return TabuSearch(
                distanceMatrix = distanceMatrix,
                n = n,
                tabuList = initialTabuList,
                tabuTenure = tabuTenure,
                tabuEliteMoves = mutableListOf(),
                maxIter = maxIter,
                maxMoves = maxMoves
            )
        }
    }
}

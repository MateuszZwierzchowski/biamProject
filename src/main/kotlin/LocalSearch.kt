import kotlin.random.Random

/**
 * Data class for returning Local Search results.
 *
 * @property bestTour      The final TSP route (list of node indices).
 * @property bestDistance  The distance of that route.
 * @property steps         How many improvement steps were made.
 * @property evaluated     How many local moves were evaluated in total.
 */
data class LocalSearchResult(
    val initialTour: List<Int>,
    val bestTour: List<Int>,
    val initialDistance: Float,
    val bestDistance: Float,
    val steps: Int,
    val evaluated: Int
)

/**
 * LocalSearch class for TSP.
 *
 * Contains algorithms like greedy local search, steepest local search, and a simple heuristic.
 *
 * @property distanceMatrix TSP distance matrix.
 * @property n              Number of nodes.
 */
class LocalSearch(
    private val distanceMatrix: List<List<Float>>
) {
    private val n: Int = distanceMatrix.size
    /**
     * Greedy local search: repeatedly make the *first* improving 2-opt swap and stop when no improvement is found.
     *
     * @return [LocalSearchResult] with the final route, distance, steps, and evaluation count.
     */
    fun greedy(): LocalSearchResult {
        var currentTour = randomPermutation(n)
        var bestTour = currentTour.toList()
        val initialTour = currentTour.toList()
        val initialDistance = calculateTourDistance(initialTour, distanceMatrix)

        var evaluated = 0
        var steps = 0

        var improvement = true
        while (improvement) {
            improvement = false
            // Explore 2-opt moves in the route
            for (i in 0 until n) {
                val nextI = (i + 1) % n
                for (j in i + 2 until n) {
                    val nextJ = (j + 1) % n
                    // Skip if edges wrap trivially
                    if (nextJ == i) continue

                    val delta = getDeltaIntraRoute(
                        distanceMatrix,
                        currentTour[i], currentTour[nextI],
                        currentTour[j], currentTour[nextJ]
                    )
                    evaluated++

                    // If this 2-opt swap improves the tour (delta < 0), we do it.
                    if (delta < 0.0f) {
                        bestTour = swap2Edges(currentTour, nextI, j, bestTour.toMutableList())
                        improvement = true
                        break
                    }
                }
                if (improvement) {
                    // Apply the swap so the new route is our current route
                    currentTour = bestTour.toList()
                    steps++
                    break
                }
            }
        }

        val distance = calculateTourDistance(currentTour, distanceMatrix)
        return LocalSearchResult(initialTour, currentTour, initialDistance, distance, steps, evaluated)
    }

    /**
     * Steepest local search: among all 2-opt moves, pick the one that yields the *largest* improvement (lowest delta).
     * Then repeat until no improvement is found.
     *
     * @return [LocalSearchResult] with the final route, distance, steps, and evaluation count.
     */
    fun steepest(): LocalSearchResult {
        var currentTour = randomPermutation(n)
        var bestTour = currentTour.toList()
        val initialTour = currentTour.toList()
        val initialDistance = calculateTourDistance(initialTour, distanceMatrix)

        var evaluated = 0
        var steps = 0

        var bestDelta = 0.0f
        var improvement = true

        while (improvement) {
            improvement = false
            // Explore all 2-opt moves
            for (i in 0 until n) {
                val nextI = (i + 1) % n
                for (j in i + 2 until n) {
                    val nextJ = (j + 1) % n
                    if (nextJ == i) continue

                    val delta = getDeltaIntraRoute(
                        distanceMatrix,
                        currentTour[i], currentTour[nextI],
                        currentTour[j], currentTour[nextJ]
                    )
                    evaluated++

                    // If this swap is better than any found so far in this pass, store it
                    if (delta < bestDelta) {
                        bestTour = swap2Edges(currentTour, nextI, j, bestTour.toMutableList())
                        bestDelta = delta
                        improvement = true
                    }
                }
            }

            // If at least one improving move was found, apply it and increment step count
            if (improvement) {
                currentTour = bestTour.toList()
                steps++
                // Reset bestDelta to search for new best improvement next iteration
                bestDelta = 0.0f
            }
        }

        val distance = calculateTourDistance(currentTour, distanceMatrix)
        return LocalSearchResult(initialTour, currentTour, initialDistance, distance, steps, evaluated)
    }

    /**
     * A simple heuristic: pick a random city, then repeatedly go to the nearest unvisited city.
     * Returns a single constructed route (not improved by 2-opt).
     *
     * @return [LocalSearchResult] with the final route, distance, steps=0, and evaluated=0.
     */
    fun heuristic(): LocalSearchResult {
        val rng = Random.Default

        val visited = MutableList(n) { false }
        val tour = mutableListOf<Int>()
        var totalDistance = 0.0f

        // Start from a random city
        var currentCity = rng.nextInt(n)
        tour.add(currentCity)
        visited[currentCity] = true

        // Greedily pick the nearest unvisited city
        while (tour.size < n) {
            var minDistance = Float.MAX_VALUE
            var nearestCity = 0
            for ((city, isVisited) in visited.withIndex()) {
                if (!isVisited && distanceMatrix[currentCity][city] < minDistance) {
                    minDistance = distanceMatrix[currentCity][city]
                    nearestCity = city
                }
            }
            currentCity = nearestCity
            tour.add(currentCity)
            visited[currentCity] = true
            totalDistance += minDistance
        }

        // Close the loop by returning to the start city
        totalDistance += distanceMatrix[tour.last()][tour.first()]

        return LocalSearchResult(tour, tour, totalDistance, totalDistance, steps = 0, evaluated = 0)
    }

    companion object {
        fun new(distanceMatrix: List<List<Float>>): LocalSearch {
            return LocalSearch(distanceMatrix)
        }
    }
}

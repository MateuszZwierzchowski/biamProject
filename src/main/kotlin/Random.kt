/**
 * A simple data class to hold the results of random-based searches.
 *
 * @property bestTour     The final/best TSP route (sequence of nodes).
 * @property bestDistance The total distance of [bestTour].
 * @property steps        The number of improvement "steps" (here always 0).
 * @property evaluated    The total count of solutions or moves evaluated.
 */
data class RandomSearchResult(
    val bestTour: List<Int>,
    val bestDistance: Float,
    val steps: Int,
    val evaluated: Int
)

/**
 * A class analogous to the Rust `Random` struct. It stores:
 * - The TSP distance matrix.
 * - The number of cities [n].
 * - The best solution [solution] found so far and its [distance].
 * - The current solution [currentSolution] and its [currentDistance].
 *
 * Methods:
 * - [initRandom] initializes a new random route.
 * - [search] performs repeated random solutions until time runs out.
 * - [walk] performs a random-walk 2-opt approach until time runs out.
 */
class RandomSearch(val distanceMatrix: List<List<Float>>) {
    val n: Int = distanceMatrix.size

    // Best solution and distance found so far
    var solution: List<Int>
    var distance: Float

    // Current solution and distance
    var currentSolution: List<Int>
    var currentDistance: Float

    init {
        // Rust's 'new' sets a random permutation as the solution.
        val initSol = randomPermutation(n)
        val initDist = calculateTourDistance(initSol, distanceMatrix)
        solution = initSol
        distance = initDist
        currentSolution = initSol
        currentDistance = initDist
    }

    /**
     * Re-initialize everything with a fresh random route.
     * Mirrors Rust's `init_random()`.
     */
    fun initRandom() {
        val randSol = randomPermutation(n)
        val randDist = calculateTourDistance(randSol, distanceMatrix)
        solution = randSol
        distance = randDist
        currentSolution = randSol
        currentDistance = randDist
    }

    /**
     * Perform repeated random sampling until the time limit is reached.
     * If a random solution is better than the current best, store it.
     *
     * @param timeLimitMs Time limit in milliseconds (as a Double).
     * @return A [RandomSearchResult] with the best solution discovered.
     */
    fun search(timeLimitMs: Double): RandomSearchResult {
        val startTime = System.currentTimeMillis()
        var evaluated = 0

        while ((System.currentTimeMillis() - startTime).toDouble() < timeLimitMs) {
            // Generate a fresh random solution
            currentSolution = randomPermutation(n)
            currentDistance = calculateTourDistance(currentSolution, distanceMatrix)
            evaluated++

            // Keep track of the best
            if (currentDistance < distance) {
                solution = currentSolution
                distance = currentDistance
            }
        }

        // We return steps=0 because this random approach doesn't do incremental "steps"
        return RandomSearchResult(solution, distance, steps = 0, evaluated = evaluated)
    }

    /**
     * Perform a random-walk 2-opt approach:
     *   - randomly pick an edge pair (i, j),
     *   - if 2-opt swap produces improvement (delta < 0), accept it.
     *
     * Runs until the specified time limit is reached.
     *
     * @param timeLimitMs Time limit in milliseconds (as a Double).
     * @return A [RandomSearchResult] with the final best solution discovered.
     */
    fun walk(timeLimitMs: Double): RandomSearchResult {
        val startTime = System.currentTimeMillis()
        var evaluated = 0

        while ((System.currentTimeMillis() - startTime).toDouble() < timeLimitMs) {
            // Get a random pair (i, j)
            var (i, j) = randomPair(n)
            // Ensure i < j
            if (i > j) {
                val temp = i
                i = j
                j = temp
            }
            val nextI = (i + 1) % n
            val nextJ = (j + 1) % n
            if (nextJ == i) {
                continue
            }

            // Compute delta from swapping edges (i->nextI) and (j->nextJ)
            val delta = getDeltaIntraRoute(
                distanceMatrix,
                solution[i],
                solution[nextI],
                solution[j],
                solution[nextJ]
            )
            evaluated++

            // If it improves (delta < 0), apply the swap
            if (delta < 0.0f) {
                solution = swap2Edges(solution, nextI, j, solution.toMutableList())
                distance += delta
            }
        }

        // steps=0 for random-walk swaps; we only track "evaluated" count
        return RandomSearchResult(solution, distance, steps = 0, evaluated = evaluated)
    }
}

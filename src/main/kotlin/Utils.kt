import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
data class Solution(
    val best_distance: Float,
    val best_solution: List<Long>,
    val distances: List<Float>,
    val runtimes: List<Long>,
    val steps: List<Int>,
    val evaluated: List<Int>
)

private val json = Json { prettyPrint = true }

/**
 * Saves the TSP solution into a JSON file under `results/<instanceName>/<algorithm>.json`.
 *
 * @param instanceName Name of the instance.
 * @param algorithm Name of the algorithm.
 * @param solutions List of tours (each is a list of node indices).
 * @param distances List of the total tour distances.
 * @param elapsedTime List of elapsed times (e.g., nanoseconds or milliseconds).
 * @param steps List of step counts.
 * @param evaluated List of how many solutions were evaluated.
 */
fun saveSolution(
    instanceName: String,
    algorithm: String,
    solutions: List<List<Int>>,
    distances: List<Float>,
    elapsedTime: List<Long>,
    steps: List<Int>,
    evaluated: List<Int>
) {
    // Find index of minimum distance
    val indexOfMinDist = distances.indices.minByOrNull { distances[it] }
        ?: throw IllegalStateException("Distances list is empty!")

    // Prepare the output directory
    val outDir = File("results/$instanceName")
    if (!outDir.exists()) {
        outDir.mkdirs()
    }

    // Prepare the JSON file
    val outFile = File(outDir, "$algorithm.json")

    val bestDistance = distances[indexOfMinDist]
    val bestSolution = solutions[indexOfMinDist].map { it.toLong() }

    // Create the data object
    val solutionData = Solution(
        best_distance = bestDistance,
        best_solution = bestSolution,
        distances = distances,
        runtimes = elapsedTime,
        steps = steps,
        evaluated = evaluated
    )

    // Serialize to JSON with pretty printing
    val jsonOutput = json.encodeToString(solutionData)
    // Write to disk
    outFile.writeText(jsonOutput)
}


/**
 * Saves the TSP solution into a JSON file under `results/<instanceName>/<algorithm>.json`.
 *
 * @param instanceName Name of the instance.
 * @param algorithm Name of the algorithm.
 * @param solutions List of tours (each is a list of node indices).
 * @param distances List of the total tour distances.
 * @param elapsedTime List of elapsed times (e.g., nanoseconds or milliseconds).
 * @param steps List of step counts.
 * @param evaluated List of how many solutions were evaluated.
 */
fun saveInitFinalSolution(
    instanceName: String,
    algorithm: String,
    solutions: List<List<Int>>,
    distances: List<Float>,
    elapsedTime: List<Long>,
    steps: List<Int>,
    evaluated: List<Int>,
    situation: String // "init" or "final"
) {
    // Find index of minimum distance
    val indexOfMinDist = distances.indices.minByOrNull { distances[it] }
        ?: throw IllegalStateException("Distances list is empty!")

    // Prepare the output directory
    val outDir = File("results/init_final/$instanceName")
    if (!outDir.exists()) {
        outDir.mkdirs()
    }

    // Prepare the JSON file
    val outFile = File(outDir, "${situation}_${algorithm}.json")

    val bestDistance = distances[indexOfMinDist]
    val bestSolution = solutions[indexOfMinDist].map { it.toLong() }

    // Create the data object
    val solutionData = Solution(
        best_distance = bestDistance,
        best_solution = bestSolution,
        distances = distances,
        runtimes = elapsedTime,
        steps = steps,
        evaluated = evaluated
    )

    // Serialize to JSON with pretty printing
    val jsonOutput = json.encodeToString(solutionData)
    // Write to disk
    outFile.writeText(jsonOutput)
}

data class Coordinate(val x: Float, val y: Float)

/**
 * Computes the Euclidean distance between two coordinates.
 *
 * @param c1 First coordinate.
 * @param c2 Second coordinate.
 * @return Euclidean distance.
 */
fun euclideanDistance(c1: Coordinate, c2: Coordinate): Float {
    return sqrt((c2.x - c1.x) * (c2.x - c1.x) + (c2.y - c1.y) * (c2.y - c1.y))
}

/**
 * Builds a distance matrix from a list of coordinates.
 *
 * @param coordinates The list of coordinates.
 * @return A 2D list of distances, where distanceMatrix[i][j] is the distance from node i to j.
 */
fun calculateDistanceMatrix(coordinates: List<Coordinate>): List<List<Float>> {
    val n = coordinates.size
    val matrix = MutableList(n) { MutableList(n) { 0.0f } }
    for (i in 0 until n) {
        for (j in 0 until n) {
            val dist = euclideanDistance(coordinates[i], coordinates[j])
            matrix[i][j] = dist
            matrix[j][i] = dist
        }
    }
    return matrix
}

/**
 * Reads a TSP instance file in the style:
 *
 *     NODE_COORD_SECTION
 *     1 X Y
 *     2 X Y
 *     ...
 *     EOF
 *
 * @param filePath Path to the TSP instance file.
 * @return The distance matrix as a List<List<Float>>.
 * @throws IOException If reading the file fails.
 */
@Throws(IOException::class)
fun readInstance(filePath: String): List<List<Float>> {
    val file = File(filePath)
    val coordinates = mutableListOf<Coordinate>()

    var readingCoordinates = false
    file.forEachLine { line ->
        when {
            line == "NODE_COORD_SECTION" -> {
                readingCoordinates = true
            }
            line == "EOF" -> {
                // Stop reading
                return@forEachLine
            }
            readingCoordinates -> {
                val parts = line.trim().split(Regex("\\s+"))
                // We expect at least: index, x, y
                if (parts.size >= 3) {
                    val x = parts[1].toFloatOrNull()
                    val y = parts[2].toFloatOrNull()
                    if (x != null && y != null) {
                        coordinates.add(Coordinate(x, y))
                    }
                }
            }
        }
    }

    return calculateDistanceMatrix(coordinates)
}

/**
 * Calculates the total distance of a given tour (sequence of node indices) by summing consecutive edges.
 * The tour is assumed to be circular (ends back at the first node).
 *
 * @param tour The tour (list of node indices).
 * @param distanceMatrix 2D list of distances.
 * @return The total distance of the tour.
 */
fun calculateTourDistance(tour: List<Int>, distanceMatrix: List<List<Float>>): Float {
    var distance = 0.0f
    for (i in tour.indices) {
        val current = tour[i]
        val next = tour[(i + 1) % tour.size]
        distance += distanceMatrix[current][next]
    }
    return distance
}

/**
 * Calculate the "intra-route" delta for a 2-opt swap. This checks the cost difference if you
 * swap edges (i -> nextI) and (j -> nextJ) to become (i -> j) and (nextI -> nextJ).
 *
 * @param distanceMatrix The distance matrix.
 * @param i Index of first node.
 * @param nextI Index of node after i.
 * @param j Index of second node.
 * @param nextJ Index of node after j.
 * @return The difference in cost if you perform this 2-opt swap.
 */
fun getDeltaIntraRoute(
    distanceMatrix: List<List<Float>>,
    i: Int,
    nextI: Int,
    j: Int,
    nextJ: Int
): Float {
    val currentCost = distanceMatrix[i][nextI] + distanceMatrix[j][nextJ]
    val swappedCost = distanceMatrix[i][j] + distanceMatrix[nextI][nextJ]
    return swappedCost - currentCost
}

/**
 * Perform a 2-opt swap on [currentTour] between edges (nextI-1 -> nextI) and (j -> j+1).
 *
 * This will reverse the nodes between [next I ... j].
 *
 * @param currentTour The current tour (list of node indices).
 * @param nextI The starting boundary for the reversal.
 * @param j The ending boundary for the reversal.
 * @param bestTour A mutable list to store the result (often a copy of currentTour).
 * @return [bestTour] after the 2-opt swap.
 */
fun swap2Edges(
    currentTour: List<Int>,
    nextI: Int,
    j: Int,
    bestTour: MutableList<Int>
): MutableList<Int> {
    // firstPart = [0..nextI)
    val firstPart = currentTour.subList(0, nextI)
    // middlePartReversed = [nextI..j].reversed()
    val middlePartReversed = currentTour.subList(nextI, j + 1).reversed()
    // lastPart = [j+1..end]
    val lastPart = currentTour.subList(j + 1, currentTour.size)

    bestTour.clear()
    bestTour.addAll(firstPart)
    bestTour.addAll(middlePartReversed)
    bestTour.addAll(lastPart)

    return bestTour
}

/**
 * Generate a random permutation of [0..n-1].
 *
 * @param n Size of the permutation.
 * @return A random permutation as a List<Int>.
 */
fun randomPermutation(n: Int): List<Int> {
    val permutation = (0 until n).toMutableList()
    permutation.shuffle(Random)
    return permutation
}

/**
 * Generate a random pair of distinct indices in [0..n-1].
 *
 * @param n Upper bound of the range of indices (exclusive).
 * @return A pair of distinct indices (x1, x2).
 */
fun randomPair(n: Int): Pair<Int, Int> {
    val x1 = Random.nextInt(n)
    val x2 = (Random.nextInt(n - 1) + 1 + x1) % n
    return Pair(x1, x2)
}

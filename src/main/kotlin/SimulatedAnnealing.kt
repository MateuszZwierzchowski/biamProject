import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * SimulatedAnnealing class for the TSP.
 *
 * @property distanceMatrix TSP distance matrix.
 * @property n              Number of nodes in the TSP.
 * @property temperature    Current temperature used in the annealing schedule.
 * @property alpha          The alpha parameter for temperature decay.
 */
class SimulatedAnnealing(
    val distanceMatrix: List<List<Float>>,
    val n: Int,
    var temperature: Double,
    val alpha: Double
) {

    /**
     * Run the entire simulated annealing procedure on the TSP problem.
     *
     * @return A tuple containing (bestTour, bestDistance, steps, evaluated):
     *   - bestTour:    The best TSP route found at the end of the run.
     *   - bestDistance:The distance of that best route.
     *   - steps:       How many actual "accepted" moves occurred.
     *   - evaluated:   How many neighbor states (2-opt moves) we evaluated in total.
     */
    fun run(): SimulatedAnnealingResult {
        var currentTemperature = temperature

        // Initial solution
        var currentTour = randomPermutation(n)
        var bestTour = currentTour.toList()

        var evaluated = 0
        var steps = 0

        // Keep iterating until we drop below a small threshold (like 0.001)
        while (currentTemperature > 0.001) {
            var accept = false

            // Intra-route neighborhood: iterate over all 2-opt swaps
            for (i in 0 until n) {
                val nextI = (i + 1) % n
                for (j in i + 2 until n) {
                    val nextJ = (j + 1) % n
                    // Skip directly adjoining edges
                    if (nextJ == i) continue

                    // Calculate delta (difference in route cost) for swapping edges
                    val delta = getDeltaIntraRoute(
                        distanceMatrix,
                        currentTour[i], currentTour[nextI],
                        currentTour[j], currentTour[nextJ]
                    )
                    evaluated++

                    // Accept if it improves the solution OR if it passes a probabilistic test
                    if (delta < 0.0 ||
                        exp(-delta.toDouble() / currentTemperature) > Random.nextDouble()
                    ) {
                        // Swap the edges
                        bestTour = swap2Edges(
                            currentTour,
                            nextI,
                            j,
                            bestTour.toMutableList()
                        )
                        accept = true
                        break
                    }
                }
                if (accept) {
                    // We actually apply the swap to currentTour
                    currentTour = bestTour.toList()
                    steps++
                    break
                }
            }

            // Exponential-like decay schedule
            currentTemperature /= (1.0 + alpha * currentTemperature)
        }

        val distance = calculateTourDistance(currentTour, distanceMatrix)
        return SimulatedAnnealingResult(
            bestTour = currentTour,
            bestDistance = distance,
            steps = steps,
            evaluated = evaluated
        )
    }

    companion object {
        fun new(distanceMatrix: List<List<Float>>): SimulatedAnnealing {
            val n = distanceMatrix.size
            // We'll call determineInitialTemperature(...) later to set it.
            return SimulatedAnnealing(distanceMatrix, n, 0.0, 0.99)
        }
    }

    /**
     * Determine initial temperature by sampling “positive” deltas for 2-opt moves
     * and setting the temperature so that a ~95% acceptance probability is given
     */
    fun determineInitialTemperature() {
        val currentTour = randomPermutation(n)
        val rng = Random.Default

        // We'll choose a random upper bound in [0..n/2)
        val sampleN = rng.nextInt(n / 2)
        var nSamples = 0
        var avgPosDelta = 0.0

        // Collect positive deltas from 2-opt moves in a partial range
        for (i in 0 until sampleN) {
            val nextI = (i + 1).coerceAtMost(n - 1)
            for (j in i + 2 until n) {
                val nextJ = (j + 1) % n
                // skip if wrap-around is trivial
                if (nextJ == i) continue

                val delta = getDeltaIntraRoute(
                    distanceMatrix,
                    currentTour[i], currentTour[nextI],
                    currentTour[j], currentTour[nextJ]
                )
                if (delta > 0.0f) {
                    avgPosDelta += delta
                    nSamples++
                }
            }
        }

        if (nSamples > 0) {
            avgPosDelta /= nSamples
            // Probability(accept) ~ 95% => p=0.95 => p = exp(-delta / T)
            // => T = - delta / ln(p)
            temperature = -avgPosDelta / ln(0.99)
        } else {
            // If we got no positive deltas (very small problem?), default
            temperature = 1.0
        }
    }
}

/**
 * Container for the results of simulated annealing:
 *   - bestTour:    The final tour (list of node indices).
 *   - bestDistance:The distance of that final tour.
 *   - steps:       How many successful swaps we made.
 *   - evaluated:   Total 2-opt pairs we evaluated during the run.
 */
data class SimulatedAnnealingResult(
    val bestTour: List<Int>,
    val bestDistance: Float,
    val steps: Int,
    val evaluated: Int
)

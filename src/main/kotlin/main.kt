import java.io.File
import kotlin.system.exitProcess

fun main() {
    // This will track an average time in ms
    var avgTime = 0.0
    // List of algorithms to run
    val algorithms = listOf("greedy", "steepest")
    val runs = 100

    val dataDir = File("./src/main/resources")
    val txtFiles = dataDir.listFiles { _, name -> name.endsWith(".txt") }
        ?: run {
            println("No .txt files found in './data' directory.")
            exitProcess(1)
        }

    // Iterate over each .txt file
    for (file in txtFiles) {
        val path = file.absolutePath
        val instanceName = file.nameWithoutExtension
        println("Processing instance: $instanceName")

        // Read TSP instance (distance matrix)
        val distanceMatrix = readInstance(path)
        // Create solver objects
        val solverLS = LocalSearch(distanceMatrix)
        val solverR  = RandomSearch(distanceMatrix)
        val solverSA = SimulatedAnnealing.new(distanceMatrix)
        val solverTS = TabuSearch.new(distanceMatrix, null)  // passing null for iters = default

        solverSA.determineInitialTemperature()

        // For each of the defined algorithms
        for (algorithm in algorithms) {
            // Collect data across multiple runs
            val elapsedTimes = mutableListOf<Long>()
            val distances = mutableListOf<Float>()
            val solutions = mutableListOf<List<Int>>()
            val initialDistances = mutableListOf<Float>()
            val initialSolutions = mutableListOf<List<Int>>()
            val stepsList = mutableListOf<Int>()
            val evaluatedList = mutableListOf<Int>()

            // We run each algorithm 'runs' times
            for (i in 1..runs) {
                solverR.initRandom()
                val timeStart = System.currentTimeMillis()

                // We'll call the appropriate method based on the algorithm name
                // Then extract (solution, distance, steps, evaluated).
                val (initialSolution, solution, initialDistance, distance, steps, evaluated) = when (algorithm) {
                    "greedy" -> {
                        val res = solverLS.greedy()
                        // res is LocalSearchResult(...)
                        Sixtuple(res.initialTour, res.bestTour, res.initialDistance, res.bestDistance, res.steps, res.evaluated)
                    }
                    "steepest" -> {
                        val res = solverLS.steepest()
                        Sixtuple(res.initialTour, res.bestTour, res.initialDistance, res.bestDistance, res.steps, res.evaluated)
                    }
                    "random_search" -> {
                        // If you want the same logic, pass avgTime as time limit in ms,
                        // or use some constant if you prefer. E.g. 100.0
                        val res = solverR.search(avgTime)
                        Sixtuple(emptyList(),  res.bestTour, null, res.bestDistance, res.steps, res.evaluated)
                    }
                    "random_walk" -> {
                        val res = solverR.walk(avgTime)
                        Sixtuple(emptyList(), res.bestTour, null, res.bestDistance, res.steps, res.evaluated)
                    }
                    "heuristic" -> {
                        val res = solverLS.heuristic()
                        Sixtuple(emptyList(), res.bestTour, null, res.bestDistance, res.steps, res.evaluated)
                    }
                    "simulated_annealing" -> {
                        val res = solverSA.run()
                        Sixtuple(emptyList(), res.bestTour, null, res.bestDistance, res.steps, res.evaluated)
                    }
                    "tabu_search" -> {
                        val res = solverTS.run()
                        // Note: TabuResult might have (bestTour, bestDistance, bestIter, evaluated)
                        // We'll treat bestIter as 'steps' for consistency
                        Sixtuple(emptyList(), res.bestTour, null, res.bestDistance, res.bestIter, res.evaluated)
                    }
                    else -> throw IllegalArgumentException("Unknown algorithm: $algorithm")
                }

                val elapsed = System.currentTimeMillis() - timeStart
                elapsedTimes.add(elapsed)
                if(initialDistance != null) {
                    initialDistances.add(initialDistance)
                }
                distances.add(distance)
                initialSolutions.add(initialSolution)
                solutions.add(solution)
                stepsList.add(steps)
                evaluatedList.add(evaluated)
            }


            saveInitFinalSolution(
                instanceName = instanceName,
                algorithm = algorithm,
                solutions = initialSolutions,
                distances = initialDistances,
                elapsedTime = elapsedTimes.map { it },
                steps = stepsList,
                evaluated = evaluatedList,
                situation = "init"
            )

            saveInitFinalSolution(
                instanceName = instanceName,
                algorithm = algorithm,
                solutions = solutions,
                distances = distances,
                elapsedTime = elapsedTimes.map { it },
                steps = stepsList,
                evaluated = evaluatedList,
                situation = "final"
            )


            // e.g. saveSolution(instanceName, algorithm, solutions, distances, elapsedTimes, stepsList, evaluatedList)
/*            saveSolution(
                instanceName = instanceName,
                algorithm = algorithm,
                solutions = solutions,
                distances = distances,
                elapsedTime = elapsedTimes.map { it },
                steps = stepsList,
                evaluated = evaluatedList
            )*/

            // Update average time based on the runs
            avgTime = elapsedTimes.sum().toDouble() / elapsedTimes.size.toDouble()
            println("\t$algorithm: $avgTime ms (average over $runs runs)")
        }
    }
}

data class Sixtuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

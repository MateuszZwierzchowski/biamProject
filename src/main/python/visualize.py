import json
from glob import glob
from os.path import join
import numpy as np
import matplotlib.pyplot as plt


def read_results(data_path):
    results = dict()
    for intance in glob(join(data_path, "*")):
        instance_name = intance.split('\\')[-1]
        if instance_name in optima.keys():
            results[instance_name] = dict()
            for algorithm in glob(join(intance, "*.json")):
                algorithm_name = algorithm.split('\\')[-1].split('.')[0]
                with open(algorithm, 'r') as file:
                    results[instance_name][algorithm_name] = json.load(file)

    return results


def quality_plot(algorithm_names, save_path):
    rows = 2
    cols = int(len(results) / 2)
    bar_width = 1

    fig, axes = plt.subplots(rows, cols, figsize = (16, 9))
    fig.tight_layout(h_pad=7)
    plt.subplots_adjust(top=0.9, bottom = 0.1)

    for i, (instance_name, algrorithms) in enumerate(results.items()):
        row = i // 4
        col = i - row * 4

        pos = np.arange(len(algorithm_names)) * bar_width

        diff_best = [algrorithms[algorithm_name]["best_distance"] - optima[instance_name] for algorithm_name in algorithm_names]
        diff_avg = [np.mean(algrorithms[algorithm_name]["distances"]) - optima[instance_name] for algorithm_name in algorithm_names]
        std_diff_avg = [np.std(algrorithms[algorithm_name]["distances"]) for algorithm_name in algorithm_names]

        axes[row, col].bar(pos, diff_best, edgecolor='white', width=bar_width)
        axes[row, col].bar(pos, diff_avg, bottom=diff_best,
                           yerr=std_diff_avg, ecolor='black', capsize=10,
                           edgecolor='white', width=bar_width)

        axes[row, col].set_title(instance_name)
        axes[row, col].set_xticks(pos)
        axes[row, col].set_xticklabels(algorithm_names, fontsize=7, rotation=45)
        axes[row, col].tick_params(axis='y', labelsize=7)

    fig.suptitle("Distance to optimum", fontsize = 32)
    fig.legend(["Best", "Average"], loc='upper right')
    plt.savefig(save_path)

def runtime_plot(algorithm_names, save_path):
    rows = 2
    cols = int(len(results) / 2)
    bar_width = 1

    fig, axes = plt.subplots(rows, cols, figsize = (16, 9))
    fig.tight_layout(h_pad=7)
    plt.subplots_adjust(top=0.9, bottom = 0.1)

    for i, (instance_name, algrorithms) in enumerate(results.items()):
        row = i // 4
        col = i - row * 4

        pos = np.arange(len(algorithm_names)) * bar_width

        runtimes_avg = [np.mean(algrorithms[algorithm_name]["runtimes"]) for algorithm_name in algorithm_names]
        runtimes_std = [np.std(algrorithms[algorithm_name]["runtimes"]) for algorithm_name in algorithm_names]

        axes[row, col].bar(pos, runtimes_avg,
                           yerr=runtimes_std, ecolor='black', capsize=10,
                           edgecolor='white', width=bar_width)

        axes[row, col].set_title(instance_name)
        axes[row, col].set_xticks(pos)
        axes[row, col].set_xticklabels(algorithm_names, fontsize=7, rotation=45)
        axes[row, col].tick_params(axis='y', labelsize=7)

    fig.suptitle("Runtime", fontsize = 32)
    plt.savefig(save_path)

def efficiency_plot(algorithm_names, save_path, weight_runtime = 0.5, weight_score = 0.5):
    def normilize(array):
        return (array - array.min()) / (array.max() - array.min())

    rows = 2
    cols = int(len(results) / 2)
    bar_width = 1

    fig, axes = plt.subplots(rows, cols, figsize = (16, 9))
    fig.tight_layout(h_pad=7)
    plt.subplots_adjust(top=0.9, bottom = 0.1)

    for i, (instance_name, algrorithms) in enumerate(results.items()):
        row = i // 4
        col = i - row * 4

        pos = np.arange(len(algorithm_names)) * bar_width

        score_avg = normilize(np.array([np.mean(algrorithms[algorithm_name]["distances"]) for algorithm_name in algorithm_names]))
        runtimes_avg = normilize(np.array([np.mean(algrorithms[algorithm_name]["runtimes"]) for algorithm_name in algorithm_names]))
        efficiency_scores = 1 - ((weight_runtime * runtimes_avg) + (weight_score * score_avg))

        axes[row, col].bar(pos, efficiency_scores, edgecolor='white', width=bar_width)

        axes[row, col].set_title(instance_name)
        axes[row, col].set_xticks(pos)
        axes[row, col].set_xticklabels(algorithm_names, fontsize=7, rotation=45)
        axes[row, col].tick_params(axis='y', labelsize=7)

    fig.suptitle(f"Efficiency with Runtime weight = {weight_runtime} and Score weight = {weight_score}", fontsize = 32)
    plt.savefig(save_path)

def step_plot(algorithm_names, save_path):
    rows = 2
    cols = int(len(results) / 2)
    bar_width = 1

    fig, axes = plt.subplots(rows, cols, figsize = (16, 9))
    fig.tight_layout(h_pad=5)
    plt.subplots_adjust(top=0.9)

    for i, (instance_name, algrorithms) in enumerate(results.items()):
        row = i // 4
        col = i - row * 4

        pos = np.arange(len(algorithm_names)) * bar_width

        steps_avg = [np.mean(algrorithms[algorithm_name]["steps"]) for algorithm_name in algorithm_names]
        steps_stds = [np.std(algrorithms[algorithm_name]["steps"]) for algorithm_name in algorithm_names]

        axes[row, col].bar(pos, steps_avg,
                           yerr=steps_stds, ecolor='black', capsize=10,
                           edgecolor='white', width=bar_width)

        axes[row, col].set_title(instance_name)
        axes[row, col].set_xticks(pos)
        axes[row, col].set_xticklabels(algorithm_names, fontsize=7)
        axes[row, col].tick_params(axis='y', labelsize=7)

    fig.suptitle("Average number of steps", fontsize = 32)
    plt.savefig(save_path)

def solution_evaluations_plot(algorithm_names, save_path):
    rows = 2
    cols = int(len(results) / 2)
    bar_width = 1

    fig, axes = plt.subplots(rows, cols, figsize = (16, 9))
    fig.tight_layout(h_pad=5)
    plt.subplots_adjust(top=0.9)

    for i, (instance_name, algrorithms) in enumerate(results.items()):
        row = i // 4
        col = i - row * 4

        pos = np.arange(len(algorithm_names)) * bar_width

        evals_avg = [np.mean(algrorithms[algorithm_name]["evaluated"]) for algorithm_name in algorithm_names]
        evals_stds = [np.std(algrorithms[algorithm_name]["evaluated"]) for algorithm_name in algorithm_names]

        axes[row, col].bar(pos, evals_avg,
                           yerr=evals_stds, ecolor='black', capsize=10,
                           edgecolor='white', width=bar_width)

        axes[row, col].set_title(instance_name)
        axes[row, col].set_xticks(pos)
        axes[row, col].set_xticklabels(algorithm_names, fontsize=7)
        axes[row, col].tick_params(axis='y', labelsize=7)

    fig.suptitle("Average number of solution evaluations", fontsize = 32)
    plt.savefig(save_path)

def init_vs_final_plot(save_path):
    results_path = join(data_path, "init_final")
    print(results_path)
    # Get all solutions
    results = dict()
    for intance in glob(join(results_path, "*")):
        instance_name = intance.split('\\')[-1]
        results[instance_name] = dict()
        for algorithm in glob(join(intance, "*")):
            algorithm_name = algorithm.split('\\')[-1].split('.')[0]
            solutions_type, algorithm_name = algorithm_name.split('_')
            if algorithm_name not in results[instance_name]:
                results[instance_name][algorithm_name] = dict()
            with open(algorithm, 'r') as file:
                results[instance_name][algorithm_name][solutions_type] = json.load(file)["distances"]

    # Plot
    fig = plt.figure(figsize=(16, 9))
    fig, axes = plt.subplots(1, len(results.keys()), figsize = (21, 7))
    axes[0].set_ylabel('Final')

    for i, instance_name in enumerate(results.keys()):
        algorithm_names = list(results[instance_name].keys())
        for algorithm_name in results[instance_name].keys():
            axes[i].scatter(results[instance_name][algorithm_name]["init"], results[instance_name][algorithm_name]["final"])
        axes[i].set_title(instance_name)
        axes[i].set_xlabel('Initial')
        axes[i].set_xlim(xmin=min(results[instance_name][algorithm_name]["final"]))

    fig.suptitle("Quality of initial vs final solution", fontsize = 32)
    fig.legend(algorithm_names, loc='upper right')
    plt.savefig(save_path)

def similarity_plot(save_path):
    results_path = join(data_path, "similarity")

    # Get all solutions and extract best ones
    results = dict()
    best_solutions = dict()
    for intance in glob(join(results_path, "*")):
        instance_name = intance.split('\\')[-1]
        results[instance_name] = dict()
        best_solutions[instance_name] = dict()
        for algorithm in glob(join(intance, "*")):
            algorithm_name = algorithm.split('\\')[-1]
            results[instance_name][algorithm_name] = [[], [], []]
            for run in glob(join(algorithm, "*.json")):
                with open(run, 'r') as file:
                    solution = json.load(file)
                    # Store fitness
                    results[instance_name][algorithm_name][0].append(solution['best_distance'])
                    # Store solution
                    results[instance_name][algorithm_name][1].append(solution['best_solution'])
            best_solution_idx = np.argmin(results[instance_name][algorithm_name][0])
            # Move best solution
            best_solutions[instance_name][algorithm_name] = results[instance_name][algorithm_name][1].pop(best_solution_idx)
            results[instance_name][algorithm_name][0].pop(best_solution_idx)

    # Get number of common edges
    def getEdges(solution):
        tmp = np.roll(solution, -1)
        return [list(edge) for edge in np.stack([solution, tmp]).T]
    def similarityEdges(edges1_with_rev, edges2):
        return len([edge for edge in edges2 if edge in edges1_with_rev])

    for instance_name in results.keys():
        for algorithm_name in results[instance_name].keys():
            best_edges = getEdges(best_solutions[instance_name][algorithm_name])
            # Calculate reversed edges
            best_edges_rev = [[edge[1], edge[0]] for edge in best_edges]
            # Concatenate
            best_edges += best_edges_rev
            for solution in results[instance_name][algorithm_name][1]:
                results[instance_name][algorithm_name][2].append(similarityEdges(best_edges_rev, getEdges(solution)))

    # Plot
    fig = plt.figure(figsize=(16, 9))
    fig, axes = plt.subplots(1, len(results.keys()), figsize = (16, 9), sharey = True)
    axes[0].set_ylabel('Number of common edges')

    for i, instance_name in enumerate(results.keys()):
        algorithm_names = list(results[instance_name].keys())
        for algorithm_name in results[instance_name].keys():
            axes[i].scatter(results[instance_name][algorithm_name][0], results[instance_name][algorithm_name][2])
        axes[i].set_title(instance_name)
        axes[i].set_xlabel('Quality')

    fig.suptitle("Similarity", fontsize = 32)
    fig.legend(algorithm_names, loc='upper right')
    plt.savefig(save_path)


data_path = "../../../results"
optima = {"berlin52": 7_542,
          "kroA100": 21_282,
          "vm1084": 239_297,
          "rat99": 1_211,
          "rat195": 2_323,
          "rat575": 6_773,
          "a280": 2_579,
          "p654": 34_643}
algorithm_names = ['greedy', 'steepest', 'heuristic', 'simulated_annealing', 'tabu_search', 'random_walk', 'random_search']


if __name__ == "__main__":
    results = read_results(data_path)

    # Quality plots
    save_path = './plots/quality_all.svg'
    quality_plot(algorithm_names, save_path)

    save_path = './plots/quality_no-RS.svg'
    quality_plot(algorithm_names[:-1], save_path)

    save_path = './plots/quality_no-RS-SA.svg'
    quality_plot(algorithm_names[:3] + algorithm_names[4:-1], save_path)

    # Runtime plot
    save_path = './plots/runtime.svg'
    runtime_plot(algorithm_names, save_path)

    # Efficiency plot
    save_path = './plots/efficiency.svg'
    efficiency_plot(algorithm_names, save_path)

    # Step plot
    save_path = './plots/steps.svg'
    step_plot(['greedy', 'steepest'], save_path)

    # Evaluations plots
    save_path = './plots/evaluations_LS.svg'
    solution_evaluations_plot(['greedy', 'steepest'], save_path)

    save_path = './plots/evaluations_RS-RW.svg'
    solution_evaluations_plot(['random_walk', 'random_search'], save_path)

    # Initial vs Final plot
    save_path = './plots/init_vs_final.svg'
    init_vs_final_plot(save_path)

    # Similarity plot
    save_path = './plots/similarity.svg'
    similarity_plot(save_path)
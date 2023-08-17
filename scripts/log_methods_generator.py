import networkx as nx
import itertools
import argparse


def log_method_refinement(call_graph_file):

    edges = []
    with open(call_graph_file) as lines:
        for line in lines:
            edges.append(line.strip())

    edges = list(set(edges))
    G = nx.DiGraph()
    logmethods = set()

    for edge in edges:
        caller = edge.split(' ')[0]
        callee = edge.split(' ')[1]
        caller = caller[2:]
        callee = callee[3:]
        G.add_edge(caller, callee)
        if 'logg' in callee.lower():
            logmethods.add(caller)

    remove_nodes = []
    for node in G.nodes:
        flag = 0
        for logmethod in logmethods:
            if nx.has_path(G, node, logmethod):
                flag = 1
        if flag == 0:
            remove_nodes.append(node)

    for node in remove_nodes:
        G.remove_node(node)

    return G


def pairwise(iterable):
    # s -> (s0,s1), (s1,s2), (s2, s3), ...
    a, b = itertools.tee(iterable)
    next(b, None)
    return list(zip(a, b))


def call_graph_construction(call_graph_file) -> object:
    edges = []
    with open(call_graph_file) as lines:
        for line in lines:
            edges.append(line.strip())

    edges = list(set(edges))
    G = nx.DiGraph()

    for edge in edges:
        caller = edge.split(' ')[0]
        callee = edge.split(' ')[1]
        caller = caller[2:]
        callee = callee[3:]
        G.add_edge(caller, callee)

    logmethods = list(log_method_refinement(call_graph_file).nodes)

    roots_temp = list(v for v, d in G.in_degree() if d == 0)

    all_paths = []
    for root in roots_temp:
        for leaf in logmethods:
            paths = nx.all_simple_paths(G, root, leaf)
            for path in paths:
                all_paths.append(path)

    logG = nx.DiGraph()
    for path in all_paths:
        pair_path = pairwise(path)
        for pair in pair_path:
            logG.add_edge(pair[0], pair[1])

    log_root_nodes = [log_method for log_method in logmethods if (log_method in roots_temp)]
    logG.add_nodes_from(log_root_nodes)
    return logG


def methods_generator(call_graph_file, output_file) -> object:
    logG = log_method_refinement(call_graph_file)
    with open(output_file, 'w') as file_object:
        for node in logG.nodes:
            file_object.write(node + '\n')


if __name__ == '__main__':

    parser = argparse.ArgumentParser(
        description="Generated Log-related method from call graph file"
    )
    parser.add_argument("--cg", type=str, help="Call graph file.")
    parser.add_argument("--output", type=str, help="Log-related methods")
    parser.add_argument("--matcher", type=str, help="Log API Matcher", default="Logger")
    args = parser.parse_args()

    if args.cg is None:
        print("WARNING: Callgraph is None.")
    if args.output is None:
        print("WARNING: output is None.")

    call_graph_file = args.cg
    output_file = args.output

    methods_generator(call_graph_file, output_file)

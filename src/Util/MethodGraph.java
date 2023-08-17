package Util;

import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;



public class MethodGraph {
    private List<Node> nodes;

    public MethodGraph() {
        this.nodes = new ArrayList<>();
    }

    public MethodGraph(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void addNode(Node e) {
        this.nodes.add(e);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public int getSize() {
        return this.nodes.size();
    }


    public Node getNode(SootMethod searchId) {
        for (Node node:this.getNodes()) {
            if (node.getId().equals(searchId)) {
                return node;
            }
        }
        return null;
    }

}

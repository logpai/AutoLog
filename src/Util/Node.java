package Util;


import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;


public class Node {
    private SootMethod id;
    private List<SootMethod> neighbors;

    public Node(SootMethod id) {
        this.id = id;
        this.neighbors = new ArrayList<SootMethod>();
    }

    public void addNeighbor(SootMethod e) {
        this.neighbors.add(e);
    }

    public SootMethod getId() {
        return id;
    }

    public List<SootMethod> getNeighbors() {
        return neighbors;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", neighbors=" + neighbors +
                "}"+ "\n";
    }
}





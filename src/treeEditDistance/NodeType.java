package treeEditDistance.costmodel;

public enum NodeType {
    Comparator, // for root node
    Constant, Field, Parameter, Class, CaughtException, // for leaf node
    BinOperators, Invoke, Array, MultiArray, UnaryOperators, InstanceOf;// for no-leaf node


    public boolean equals(NodeType other) {
        return this == other;
    }

}
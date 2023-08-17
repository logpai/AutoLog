/* MIT License
 *
 * Copyright (c) 2017 Mateusz Pawlik
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package treeEditDistance.node;

import soot.SootField;
import soot.Type;
import soot.Value;
import soot.jimple.*;
import treeEditDistance.costmodel.NodeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static treeEditDistance.costmodel.NodeType.Array;

/**
 * This is a recursive representation of an ordered tree. Each node stores a
 * list of pointers to its children. The order of children is significant and
 * must be observed while implmeneting a custom input parser.
 *
 * @param <D> the type of node data (node label).
 */
public class Node<D> implements Cloneable {

    /**
     * Information associated to and stored at each node. This can be anything
     * and depends on the application, for example, string label, key-value pair,
     * list of values, etc.
     */
    private D nodeData;

    /**
     * Array of pointers to this node's children. The order of children is
     * significant due to the definition of ordered trees.
     */
    private List<Node<D>> children;

    /**
     * Constructs a new node with the passed node data and an empty list of
     * children.
     *
     * @param nodeData instance of node data (node label).
     */
    public Node(D nodeData) {
        this.children = new ArrayList<>();
        setNodeData(nodeData);
    }

    /**
     * Counts the number of nodes in a tree rooted at this node.
     *
     * <p>This method runs in linear time in the tree size.
     *
     * @return number of nodes in the tree rooted at this node.
     */
    public int getNodeCount() {
        int sum = 1;
        for (Node<D> child : getChildren()) {
            sum += child.getNodeCount();
        }
        return sum;
    }

    /**
     * Adds a new child at the end of children list. The added child will be
     * the last child of this node.
     *
     * @param c child node to add.
     */
    private void addChildNode(Node<D> c) {
        this.children.add(c);
    }


    public void addChild(Type t) {
        String clazz = t.toString();
        PredicateNodeData data = new PredicateNodeData(NodeType.Class, clazz);
        Node<PredicateNodeData> node = new Node<>(data);
        addChildNode((Node<D>) node);
    }

    public void addChild(Value v) {
        if (v instanceof IntConstant) {
            int num = ((IntConstant) v).value;
            PredicateNodeData data = new PredicateNodeData(NodeType.Constant, Integer.toString(num) );
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof StringConstant) {
            String str = ((StringConstant) v).value;
            if (str.equals(""))
                str = " ";
            PredicateNodeData data = new PredicateNodeData(NodeType.Constant,  str);
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof LongConstant) {
            long num = ((LongConstant) v).value;
            PredicateNodeData data = new PredicateNodeData(NodeType.Constant, Long.toString(num));
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof FloatConstant) {
            float num = ((FloatConstant) v).value;
            PredicateNodeData data = new PredicateNodeData(NodeType.Constant, Float.toString(num));
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof DoubleConstant) {
            double num = ((DoubleConstant) v).value;
            PredicateNodeData data = new PredicateNodeData(NodeType.Constant, Double.toString(num));
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof NullConstant) {
            PredicateNodeData data = new PredicateNodeData(NodeType.Constant, "null");
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof ClassConstant) {
            String clazz = v.getType().toString();
            PredicateNodeData data = new PredicateNodeData(NodeType.Class, clazz);
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof CaughtExceptionRef) {
            PredicateNodeData data = new PredicateNodeData(NodeType.CaughtException, "@caughtexception");
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof FieldRef) {
            SootField field = ((FieldRef) v).getField();
            String fieldType = field.getName();
            StringBuilder fieldSb = new StringBuilder();
            fieldSb.append(field.getDeclaringClass().getName()).append(".");
            fieldSb.append(fieldType);
            PredicateNodeData data = new PredicateNodeData(NodeType.Field,
                    fieldSb.toString());
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else if (v instanceof ParameterRef) {
            int index = ((ParameterRef) v).getIndex();
            PredicateNodeData data = new PredicateNodeData(NodeType.Parameter, "arg" + index);
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        } else {
            PredicateNodeData data = new PredicateNodeData(v);
            Node<PredicateNodeData> node = new Node<>(data);
            addChildNode((Node<D>) node);
        }
    }

    /**
     * Returns a string representation of the tree in bracket notation.
     *
     * @return tree in bracket notation.
     */
    public String toString() {
        StringBuilder res = new StringBuilder("{" + ((PredicateNodeData) getNodeData()).getData());
        for (Node<D> child : getChildren()) {
            res.append(child.toString());
        }
        res.append("}");
        return res.toString();
    }

    /**
     * Returns node data. Used especially for calculating rename cost.
     *
     * @return node data (label of a node).
     */
    public D getNodeData() {
        return nodeData;
    }

    /**
     * Sets the node data of this node.
     *
     * @param nodeData instance of node data (node label).
     */
    public void setNodeData(D nodeData) {
        this.nodeData = nodeData;
    }

    /**
     * Returns the list with all node's children.
     *
     * @return children of the node.
     */
    public List<Node<D>> getChildren() {
        return children;
    }

    public void setTo(Node<D> other) {
        this.nodeData = other.nodeData;
        this.children = new ArrayList<>();
        other.children.forEach(x -> this.children.add(x.clone()));
    }

    public void traverse(Node<PredicateNodeData> ast,
                         Value left, Node<PredicateNodeData> right) {
        PredicateNodeData data = ast.getNodeData();
        if (data.getNodeType() == null && data.getValue().equals(left)) {
            ast.setTo(right);
//            data.setValue(right);
        } else {
            for (Node<PredicateNodeData> child : ast.getChildren()) {
                traverse(child, left, right);
            }
        }
    }

    public void traverse(Node<PredicateNodeData> ast,
                         Node<PredicateNodeData> left, Node<PredicateNodeData> right) {
        PredicateNodeData data = ast.getNodeData();
        if (data.getNodeType() == null && ast.equals(left)) {
            ast.setTo(right);
        } else {
            for (Node<PredicateNodeData> child : ast.getChildren()) {
                traverse(child, left, right);
            }
        }
    }


    public void traverse(Node<PredicateNodeData> ast, HashMap<Value, Node<PredicateNodeData>> local) {
        PredicateNodeData data = ast.getNodeData();
        if (data.getNodeType() == null && local.containsKey(data.getValue())) {
            ast.setTo(local.get(data.getValue()));
        } else {
            for (Node<PredicateNodeData> child : ast.getChildren()) {
                traverse(child, local);
            }
        }
    }


    public String extractNodeData(Node<PredicateNodeData> ast) {
        if (ast != null) {
            PredicateNodeData data = ast.getNodeData();
            NodeType type = data.getNodeType();
            if (type.equals(NodeType.Comparator) || type.equals(NodeType.BinOperators)) {
                return extractNodeData(ast.children.get(0)) + data.getData() + extractNodeData(ast.children.get(1));
            } else if (type.equals(NodeType.Constant)) {
                return data.getData();
            } else if (type.equals(NodeType.UnaryOperators)) {
                return extractNodeData(ast.children.get(0));
            }
        }
        return "";
    }

    @Override
    public Node<D> clone() {
        try {
            return (Node<D>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
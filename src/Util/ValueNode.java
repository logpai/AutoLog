package Util;

import soot.Value;

import java.util.ArrayList;



public class ValueNode {
    private Value localID = null;
    private Value v = null;
    private ArrayList<ValueNode> children = new ArrayList<ValueNode>();
    private boolean flagNode = false;

    public void setLocalID(Value v){
        this.localID = v;
    }

    public void addChildren(ValueNode child){
        this.children.add(child);
    }

    public void setValue(Value v) {
        this.v = v;
    }

    public void setFlagNode(Boolean b){this.flagNode = b;}

    public Value getLocalID(){
        return this.localID;
    }

    public Value getValue() {
        return this.v;
    }

    public ArrayList<ValueNode> getChildren(){
        return this.children;
    }

    public boolean getFlagNode() { return this.flagNode;}

    public ValueNode getChildrenByIndex(int ChildIndex){
        return this.children.get(ChildIndex);
    }
}

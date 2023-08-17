package Util;


import Util.ValueNode;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ConditionExpr;
import soot.jimple.internal.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;


public class ConstraintUtils {

    public static ConditionExpr neOperand(ConditionExpr condition) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Value left = condition.getOp1();
        Value right = condition.getOp2();
        ConditionExpr value = null;
        String conditionClzName = condition.getClass().toString();
        if(conditionClzName.equals("class soot.jimple.internal.JEqExpr")){
            value = new JNeExpr(left, right);
        }
        else if(conditionClzName.equals("class soot.jimple.internal.JGeExpr")){
            value = new JLtExpr(left, right);
        }

        else if(conditionClzName.equals("class soot.jimple.internal.JGtExpr")){
            value = new JLeExpr(left, right);
        }
        else if(conditionClzName.equals("class soot.jimple.internal.JLeExpr")){
            value = new JGtExpr(left, right);
        }
        else if(conditionClzName.equals("class soot.jimple.internal.JLtExpr")){
            value = new JGeExpr(left, right);
        }
        else if(conditionClzName.equals("class soot.jimple.internal.JNeExpr")){
            value = new JEqExpr(left, right);
        }
        return value;

    }

    public static String getStringCondition(Value v, ArrayList<ValueNode> conditionTrees){
        String conditionString = v.toString();
        String conditionSub = null;
        List<ValueBox> conditionBox = v.getUseBoxes();
        for(ValueBox valueBoxCondition:conditionBox){
            if(JimpleLocal.class.isInstance((Value) valueBoxCondition.getValue())){
                for(ValueNode conditionTree:conditionTrees){
                    if(conditionTree.getLocalID()==valueBoxCondition.getValue()){
                        String conditionLocalName = valueBoxCondition.getValue().toString();
                        //String conditionValue = buildConditionString(conditionTree);
                        String conditionValue = "";                                                 // Turn off.
                        conditionSub = conditionString.replace(conditionLocalName, conditionValue);
                    }
                }
            }
        }
        return conditionSub;
    }

    public static String buildConditionString(ValueNode conditionNode){
        if(conditionNode.getChildren().size() == 0){
            return conditionNode.getValue().toString();
        }
        String originalExp = conditionNode.getValue().toString();
        String newExp = "";
        for(ValueNode conditonChild:conditionNode.getChildren()){
            String childName = conditonChild.getLocalID().toString();
            String childTrueValue = buildConditionString(conditonChild);
            newExp =originalExp.replace(childName, childTrueValue);
        }
        return newExp;
    }


}

package analyseDepth.analyzer;

import analyseDepth.path.BlocksAlongHops;
import analyseDepth.path.OneHop;
import analyseDepth.customClass.ConditionResult;
import soot.Unit;
import soot.Value;

import soot.jimple.ConditionExpr;
import analyseDepth.customClass.ValueNode;
import soot.jimple.IntConstant;
import soot.jimple.Stmt;
import soot.jimple.internal.*;
import soot.toolkits.graph.Block;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static analyseDepth.utils.ConstraintUtils.getStringCondition;
import static analyseDepth.utils.ConstraintUtils.neOperand;
import static analyseDepth.utils.PathUtils.getShortesPathMtd2Mtd;



public class ConstraintAnalyzer {
    public ArrayList<ArrayList<ConditionResult>>  getConstraintAlongPath(ArrayList<ArrayList<OneHop>> allCallPath) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException  {
        ArrayList<BlocksAlongHops> allCallStackBlocksSelected = getShortesPathMtd2Mtd(allCallPath);
        ArrayList<ArrayList<ConditionResult>> allPathCondition = new ArrayList<>();

        ArrayList<ArrayList<String>> allPathConditionString = new ArrayList<>();

        for(BlocksAlongHops blocksAlongHops:allCallStackBlocksSelected){
            ArrayList<ConditionResult> pathCondition = new ArrayList<>();
            ArrayList<String> pathConditionString = new ArrayList<>();
            int blockIndex = 0;

            for(Block block:blocksAlongHops.getPathBlock()){
                //Grantee not the last block

                if(blockIndex < blocksAlongHops.getPathBlock().size() - 1){

                    Value vv = null;

                    HashMap<Block, ArrayList<ValueNode>> localMap = blocksAlongHops.getLocalMap();
                    ArrayList<ValueNode> conditionTrees = localMap.get(block);
                    if(conditionTrees.size() != 0 && conditionTrees.get(0).getFlagNode()){                                                    //Check whether this block is the last block of the shortest path, if it is, its constraint(goto condition) dose not matter.
                        continue;
                    }

                    Unit lastUnit = block.getTail();
                    String nextBlockFirstUnit = blocksAlongHops.getPathBlock().get(blockIndex + 1).getHead().toString();

                    //If the last unit of basic block is if statement
                    if(JIfStmt.class.isInstance((Stmt) lastUnit)){
                        String targetUnit = ((JIfStmt) lastUnit).getTarget().toString();

                        if(nextBlockFirstUnit.equals(targetUnit)){
                            ConditionExpr v = (ConditionExpr) ((JIfStmt) lastUnit).getCondition();


                            //Map condition variable to value
                            String conditionString = getStringCondition(v, conditionTrees);
                            pathConditionString.add(conditionString);

                            //new condition result
                            ConditionResult conditionResulttmp = new ConditionResult();
                            conditionResulttmp.setCondition(v);
                            conditionResulttmp.setValue(conditionTrees);
                            pathCondition.add(conditionResulttmp);

                            vv = v;
                        }
                        else{
                            ConditionExpr v = (ConditionExpr) ((JIfStmt) lastUnit).getCondition();
                            ConditionExpr vNe = neOperand(v);

                            //Map condition variable to value
                            String conditionString = getStringCondition(vNe, conditionTrees);
                            pathConditionString.add(conditionString);

                            //new condition result
                            ConditionResult conditionResulttmp = new ConditionResult();
                            conditionResulttmp.setCondition(vNe);
                            conditionResulttmp.setValue(conditionTrees);
                            pathCondition.add(conditionResulttmp);

                            vv = v;
                        }
                    }

                    //If the last unit of basic block is switch case statement, in table switch format
                    else if(JTableSwitchStmt.class.isInstance((Stmt) lastUnit)){
                        JTableSwitchStmt table = (JTableSwitchStmt) lastUnit;
                        Value key = table.getKey();
                        int low = table.getLowIndex();
                        int high = table.getHighIndex();
                        for (int i = low; i < high; i++) {
                            if (table.getTarget(i - low).toString().equals(nextBlockFirstUnit)) {
                                ConditionExpr v = new JEqExpr(key, IntConstant.v(i));

                                //Map condition variable to value
                                String conditionString = getStringCondition(v, conditionTrees);
                                pathConditionString.add(conditionString);

                                //new condition result
                                ConditionResult conditionResulttmp = new ConditionResult();
                                conditionResulttmp.setCondition(v);
                                conditionResulttmp.setValue(conditionTrees);
                                pathCondition.add(conditionResulttmp);

                                vv = v;
                            }
                        }
                        if(table.getDefaultTarget().toString().equals(nextBlockFirstUnit)) {
                            // TODO: Use a range to cover the default target of the switch table
                        }

                    }

                    //If the last unit of basic block is switch case statement, in look up switch format
                    else if(JLookupSwitchStmt.class.isInstance((Stmt) lastUnit)){
                        JLookupSwitchStmt directory = (JLookupSwitchStmt) lastUnit;
                        Value key = directory.getKey();
                        List<IntConstant> caseList = directory.getLookupValues();
                        for (int i = 0; i < caseList.toArray().length; i++) {
                            if (directory.getTarget(i).toString().equals(nextBlockFirstUnit)) {
                                ConditionExpr v = new JEqExpr(key, IntConstant.v(i));

                                //Map condition variable to value
                                String conditionString = getStringCondition(v, conditionTrees);
                                pathConditionString.add(conditionString);

                                //new condition result
                                ConditionResult conditionResulttmp = new ConditionResult();
                                conditionResulttmp.setCondition(v);
                                conditionResulttmp.setValue(conditionTrees);
                                pathCondition.add(conditionResulttmp);
                                vv = v;
                            }
                        }
                        if(directory.getDefaultTarget().toString().equals(nextBlockFirstUnit)) {
                            // TODO: Use a range to cover the default target of the switch table
                        }
                    }
                    if(vv!=null){
                        int a = 0;
                        a = 1;
                    }
                }
                blockIndex = blockIndex + 1;

            }
//            if(pathCondition.size() == 0){
//                int a = 1;
//            }
            allPathCondition.add(pathCondition);
            allPathConditionString.add(pathConditionString);
        }
        return allPathCondition;
    }

    public void printPathConditionLength(ArrayList<ArrayList<ConditionResult>> allPathCondition){
        int index = 0;
        for(ArrayList<ConditionResult> pathConditionResult:allPathCondition){
            if(index == allPathCondition.size()-1){
                System.out.println(pathConditionResult.size());
            }
            else{
                System.out.print(pathConditionResult.size() + ";");
            }
            index = index + 1;
        }
    }

    public void printPathConditionVariableType(ArrayList<ArrayList<ConditionResult>> allPathCondition){
        for(ArrayList<ConditionResult> pathConditionResult:allPathCondition){
            int index = 0;
            ArrayList<String> conditionVariableTypeAlongPath = getConditionVariableType(pathConditionResult);
            for(String conditionVariableType:conditionVariableTypeAlongPath){
                if(index == conditionVariableTypeAlongPath.size() - 1){
                    System.out.println(conditionVariableType);
                }
                else{
                    System.out.print(conditionVariableType + ";");
                }
                index = index + 1;
            }
        }
    }

    public void printPathConditionOperand(ArrayList<ArrayList<ConditionResult>> allPathCondition){
        for(ArrayList<ConditionResult> pathConditionResult:allPathCondition){
            int index = 0;
            for(ConditionResult conditionVariableType:pathConditionResult){
                if(index == pathConditionResult.size() - 1){
                    System.out.println(conditionVariableType.getCondition().getClass().toString());
                }
                else{
                    System.out.print(conditionVariableType.getCondition().getClass().toString()+";");
                }
                index = index + 1;
            }
        }
    }

    public void printPathConditionInfo(ArrayList<ArrayList<ConditionResult>> allPathCondition){

    }

    public ArrayList<String> getConditionVariableType(ArrayList<ConditionResult> pathConditionResult){
        ArrayList<String> conditionVariableType = new ArrayList<>();
        for(ConditionResult conditionResult:pathConditionResult){
            for(ValueNode valueNode:conditionResult.getValue()){
                conditionVariableType.add(valueNode.getValue().getClass().toString());
            }
        }
        return conditionVariableType;
    }




}

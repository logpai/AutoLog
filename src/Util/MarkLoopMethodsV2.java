package Util;

import soot.*;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.*;

import java.util.*;

public class MarkLoopMethodsV2 {

    public Set<String> methodsInLoop = new HashSet<>();

    public MarkLoopMethodsV2(List<List<Unit>> realUnitsPaths,ArrayList<SootMethod> callRealMethods,HashMap<Value,Value> L2R_map)
    {
        for(List<Unit> unitPath:realUnitsPaths)
        {
            for(Unit unit:unitPath)
            {
                if (unit instanceof GotoStmt || unit instanceof IfStmt)
                {
                    Unit targetUnit = null;
                    if (unit instanceof GotoStmt)
                    {
                        targetUnit = ((GotoStmt) unit).getTarget();
                    }
                    else
                    {
                        targetUnit = ((IfStmt) unit).getTarget();
                    }
                    int index = unitPath.indexOf(unit);
                    int targetIndex = unitPath.indexOf(targetUnit);
                    if (targetIndex<index&&targetIndex!=-1)
                    {
                        for(int unitIndex = targetIndex; unitIndex < index; unitIndex++)
                        {
                            Unit refUnit = unitPath.get(unitIndex);
                            if (JInvokeStmt.class.isInstance(refUnit) || JAssignStmt.class.isInstance( refUnit)) {
                                SootMethod refMtd = null;
                                try {
                                    refMtd = ((Stmt) refUnit).getInvokeExpr().getMethod();
                                    if(callRealMethods.contains(refMtd)) {
                                        methodsInLoop.add(refMtd.getSignature());
                                    }
                                }
                                catch (RuntimeException e){
                                    continue;
                                }
                            }
                            if(unit.toString().toLowerCase().contains("log"))
                            {
                                if(!(unit instanceof JInvokeStmt))
                                {
                                    //System.out.println(unit.toString());
                                }
                                else{
                                    JInvokeStmt caller = (JInvokeStmt) unit;
                                    Value LogArg = caller.getInvokeExpr().getArg(0);
                                    String LoggingLevel = caller.getInvokeExpr().getMethodRef().getName();
                                    if (LogArg instanceof StringConstant)
                                    {
                                        methodsInLoop.add(LogArg.toString());
                                    }
                                    else if (LogArg instanceof JimpleLocal)
                                    {
                                        // Start recursively build the string
                                        String current_string = "";
                                        List<String> current_list = new ArrayList<>();
                                        Value current_box = LogArg;
                                        int stopper = 0;
                                        while(L2R_map.keySet().contains(current_box)&&stopper<=50)
                                        {
                                            JVirtualInvokeExpr vie = (JVirtualInvokeExpr) L2R_map.get(current_box);
                                            List<String> argsString = new ArrayList<>();
                                            for (Value arg:vie.getArgs())
                                            {
                                                argsString.add(arg.toString());
                                            }
                                            current_string = current_string+String.join(" ", argsString);
                                            if (current_string.isEmpty()) {
                                                int a=1;
                                            }
                                            current_list.add(String.join(" ", argsString));
                                            current_box = vie.getBaseBox().getValue();
                                            stopper+=1;
                                        }
                                        Collections.reverse(current_list);
                                        if (!current_list.isEmpty())
                                        {
                                            methodsInLoop.add(String.join(" ",current_list));
                                        }

                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

}

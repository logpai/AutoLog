package Util;


import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Method2LogString {

    public static HashMap<Value,Value> L2R(SootMethod MyMethod)
    {
        HashMap<Value,Value> L2R_map = new HashMap<Value,Value>();
        for(Unit unit: MyMethod.getActiveBody().getUnits())
        {
            if (unit instanceof JAssignStmt)
            {
                if(((JAssignStmt) unit).getRightOpBox().getValue() instanceof JVirtualInvokeExpr)
                {
                    JVirtualInvokeExpr vie = (JVirtualInvokeExpr) ((JAssignStmt) unit).getRightOpBox().getValue();
                    L2R_map.put(((JAssignStmt) unit).getLeftOpBox().getValue(),((JAssignStmt) unit).getRightOpBox().getValue());
                }
            }
        }
        return L2R_map;
    }

    public static List<List<String>> Method2LogString(SootMethod MyMethod) {
        List<List<String>> LevelLogPairs = new ArrayList<>();
        List<String> LogStrings = new ArrayList<>();
        List<String> LevelStrings = new ArrayList<>();
        HashMap<Value,Value> L2R_map = L2R( MyMethod);
        for(Unit unit: MyMethod.getActiveBody().getUnits()) {
            //if(unit.toString().toLowerCase().contains("void") && unit.toString().toLowerCase().contains("log"))
            if(unit.toString().toLowerCase().contains("log"))
            {
                if(!(unit instanceof JInvokeStmt))
                {
                    //System.out.println(unit.toString());
                }
                else{
                    List<String> LevelLogPair = new ArrayList<>();
                    JInvokeStmt caller = (JInvokeStmt) unit;
                    Value LogArg = caller.getInvokeExpr().getArg(0);
                    String LoggingLevel = caller.getInvokeExpr().getMethodRef().getName();
                    if (LogArg instanceof StringConstant)
                    {
                        LevelLogPair.add(LogArg.toString());
                        LogStrings.add(LogArg.toString());
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
                            LevelLogPair.add(String.join(" ",current_list));
                            LogStrings.add(String.join(" ",current_list));
                        }

                    }
                    LevelLogPair.add(LoggingLevel);
                    LevelStrings.add(LoggingLevel);
                    LevelLogPairs.add(LevelLogPair);
                }
            }
        }
        return LevelLogPairs;
    }
}

package Util;

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



public class Path2LogString {
    public static HashMap<Value,Value> L2R(List<Unit> realUnitsPath)
    {
        HashMap<Value,Value> L2R_map = new HashMap<Value,Value>();
        for(Unit unit: realUnitsPath)
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
    public static List<String> Path2LogString(List<Unit> realUnitsPath) {
        List<String> LogStrings = new ArrayList<>();
        HashMap<Value,Value> L2R_map = L2R(realUnitsPath);
        //ToDo: implement this.
        for(Unit unit: realUnitsPath) {
            if(unit.toString().toLowerCase().contains("void") && unit.toString().toLowerCase().contains("log"))
            {
                if(!(unit instanceof JInvokeStmt))
                {
                    System.out.println(unit.toString());
                }
                else{
                    JInvokeStmt caller = (JInvokeStmt) unit;
                    Value LogArg = caller.getInvokeExpr().getArg(0);
                    if (LogArg instanceof StringConstant)
                    {
                        LogStrings.add(LogArg.toString());
                    }
                    else if (LogArg instanceof JimpleLocal)
                    {
                        // Start recursively build the string
                        String current_string = "";
                        List<List> current_list = new ArrayList<>();
                        Value current_box = LogArg;
                        int stopper = 0;
                        while(L2R_map.keySet().contains(current_box)&&stopper<=50)
                        {
                            JVirtualInvokeExpr vie = (JVirtualInvokeExpr) L2R_map.get(current_box);
                            current_string = current_string+vie.getArgs().toString();
                            current_list.add(vie.getArgs());
                            current_box = vie.getBaseBox().getValue();
                            stopper+=1;
                        }
                        Collections.reverse(current_list);
                        LogStrings.add(current_list.toString());
                    }
                }
            }
        }
        return LogStrings;
    }
}

package Util;

import analyseDepth.utils.CallUtils;
import com.sun.xml.internal.ws.util.StringUtils;
import jdk.nashorn.internal.ir.Block;
import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.ExceptionalBlockGraph;
import Util.MarkLoopMethodsV2;

import java.util.*;

import Util.MethodDigest2;

public class Method2CallPath2 {

    public Set<List<String>> callPaths = new HashSet<List<String>>();
    public Set<String> methodsInLoop = new HashSet<>();


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


    public  Method2CallPath2(SootMethod origin, ArrayList<SootMethod> callRealMethods)
    {
        Body b = origin.getActiveBody();
        MethodDigest2 MD = new MethodDigest2(b);
        List<List<Unit>> realUnitsPaths = MD.realUnitsPaths;
        HashMap<Value,Value> L2R_map = L2R( origin);

        MarkLoopMethodsV2 MLM = new MarkLoopMethodsV2(realUnitsPaths,callRealMethods,L2R_map);
        methodsInLoop = MLM.methodsInLoop;

        for(List<Unit>unitPath:realUnitsPaths)
        {
            List<String> callPath = new LinkedList<>();
            for(Unit unit: unitPath)
            {
                if (JInvokeStmt.class.isInstance(unit) || JAssignStmt.class.isInstance(unit)) {
                    SootMethod refMtd = null;
                    try {
                        refMtd = ((Stmt) unit).getInvokeExpr().getMethod();
                        if(callRealMethods.contains(refMtd)) {
                            callPath.add(refMtd.getSignature());
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
                            callPath.add(LogArg.toString());
                        }
                        else if (LogArg instanceof JimpleLocal)
                        {
                            // Start recursively build the string
                            String current_string = "";
                            List<String> current_list = new ArrayList<>();
                            Value current_box = LogArg;
                            int stopper = 0;
                            while(L2R_map.keySet().contains(current_box)&&stopper<=2000)
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
                                callPath.add(String.join(" ",current_list));
                            }

                        }
                    }
                }
            }
            if(!callPath.isEmpty())
            {
                this.callPaths.add(callPath);
            }

        }
    }
}

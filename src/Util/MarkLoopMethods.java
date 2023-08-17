package Util;

import soot.*;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.*;

import java.util.*;

public class MarkLoopMethods {

    public Set<String> methodsInLoop = new HashSet<>();

    public MarkLoopMethods(List<List<Unit>> realUnitsPaths,ArrayList<SootMethod> MethodList)
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
                                    if(MethodList.contains(refMtd)) {
                                        methodsInLoop.add(refMtd.getSignature());
                                    }

                                }
                                catch (RuntimeException e){
                                    continue;
                                }
                            }
                        }
                    }
                }

            }
        }
    }

}

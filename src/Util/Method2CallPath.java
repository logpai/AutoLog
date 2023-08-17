package Util;

import analyseDepth.utils.CallUtils;
import jdk.nashorn.internal.ir.Block;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.ExceptionalBlockGraph;
import Util.MarkLoopMethods;

import java.util.*;

import Util.MethodDigest;

public class Method2CallPath {
    public Set<List<SootMethod>> callPaths = new HashSet<List<SootMethod>>();
    public Set<String> methodsInLoop = new HashSet<>();


    public  Method2CallPath(SootMethod origin,ArrayList<SootMethod> MethodList)
    {
        Body b = origin.getActiveBody();
        MethodDigest MD = new MethodDigest(b);
        List<List<Unit>> realUnitsPaths = MD.realUnitsPaths;

        MarkLoopMethods MLM = new MarkLoopMethods(realUnitsPaths, MethodList);
        methodsInLoop = MLM.methodsInLoop;

        for(List<Unit>unitPath:realUnitsPaths)
        {
            List<SootMethod> callPath = new LinkedList<>();
            for(Unit unit: unitPath)
            {
                if (JInvokeStmt.class.isInstance(unit) || JAssignStmt.class.isInstance(unit)) {
                    SootMethod refMtd = null;
                    try {
                        refMtd = ((Stmt) unit).getInvokeExpr().getMethod();
                        if(MethodList.contains(refMtd)) {
                            callPath.add(refMtd);
                        }
                    }
                    catch (RuntimeException e){
                        continue;
                    }
                }
            }
            if(!callPath.isEmpty())
            {
                this.callPaths.add(callPath);
            }

        }
    }

    public static void findNextMethod(SootMethod origin, CallUtils.NextMethod nmd, String signature){
        /**
         *
         * @Description Find the next method in call stack
         * @Param [origin, signature]
         * @return Integer
         **/
        Body body = null;
        try {
            body = origin.getActiveBody();
        }
        catch (Throwable e){
            nmd.setSm(null);
        }
        ExceptionalBlockGraph bg = new ExceptionalBlockGraph(body);
        List<soot.toolkits.graph.Block> blocks = bg.getBlocks();
        ArrayList<Integer> refBlocks = new ArrayList<>();
        for(int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
            for (Unit unit : blocks.get(blockIndex)) {
                if (JInvokeStmt.class.isInstance((Stmt) unit) || JAssignStmt.class.isInstance((Stmt) unit)) {
                    SootMethod refMtd = null;
                    try {
                        refMtd = ((Stmt) unit).getInvokeExpr().getMethod();
                    }
                    catch (RuntimeException e){
                        continue;
                    }

                    String refSignature = getRefSignature(refMtd);
                    if (refSignature.equals(signature)) {
                        nmd.setSm(refMtd);
                        refBlocks.add(blockIndex);
                    }
                }
            }
        }
        nmd.setRefBlockIndex(refBlocks);
    }

    private static String getRefSignature(SootMethod refMtd){
        /**
         *
         * @Description Get method signature in the same format of BCEL(https://commons.apache.org/proper/commons-bcel/);
         * @Param [refMtd]
         * @return java.lang.String
         */
        StringBuilder refSignature = new StringBuilder();
        String ClsName = refMtd.getDeclaringClass().toString();
        refSignature.append(ClsName);
        refSignature.append(":");
        refSignature.append(refMtd.getName());
        refSignature.append("(");
        List<Type> refPara = refMtd.getParameterTypes();
        if (refPara != null) {
            for(int i = 0; i < refPara.size(); ++i) {
                refSignature.append(((Type)refPara.get(i)).toQuotedString());
                if (i < refPara.size() - 1) {
                    refSignature.append(",");
                }
            }
        }

        refSignature.append(")");
        return refSignature.toString();
    }


    public static class NextMethod{
        private SootMethod sm;
        private ArrayList<Integer> refBlockIndex;

        public SootMethod getSm(){
            return this.sm;
        }
        public void setSm(SootMethod sm) {
            this.sm = sm;
        }
        public void setRefBlockIndex(ArrayList<Integer> refBlockIndex) {
            this.refBlockIndex = refBlockIndex;
        }

        public ArrayList<Integer> getRefBlockIndex() {
            return refBlockIndex;
        }
    }

}

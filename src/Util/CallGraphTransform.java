package Util;

import jas.Method;
import soot.Body;
import soot.BodyTransformer;
import soot.SootMethod;
import Util.MethodDigest.*;
import soot.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CallGraphTransform extends BodyTransformer {
    private static Map<SootMethod,MethodDigest> MethodMap;

    public CallGraphTransform(Map<SootMethod,MethodDigest> MethodMap) {
        this.MethodMap = MethodMap;
    }
    //Map

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        SootMethod method = b.getMethod();
        MethodDigest MD = new MethodDigest(b);
        List<List<Unit>> test = MD.realUnitsPaths;
        if(!MD.realUnitsPaths.isEmpty())
        {
            MethodMap.put(method,MD);
        }
    }
}

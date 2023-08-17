package Util;

import soot.Body;
import soot.BodyTransformer;
import soot.SootMethod;
import soot.Unit;

import java.util.List;
import java.util.Map;

public class CallGraphTransformV2 extends BodyTransformer {
    private static Map<SootMethod,MethodDigest> MethodMap;

    public CallGraphTransformV2(Map<SootMethod,MethodDigest> MethodMap) {
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

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
import java.util.Set;

public class CallGraphCounter extends BodyTransformer {
    private static Set<String> MethodSet;

    public CallGraphCounter(Set<String> MethodSet) {
        this.MethodSet = MethodSet;
    }
    //Map

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        SootMethod method = b.getMethod();
        MethodSet.add(method.getSignature());
    }
}

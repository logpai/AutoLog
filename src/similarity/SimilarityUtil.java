package similarity;


import analyze.ClassAttr;
import analyze.MethodAttr;

import java.util.List;

public class SimilarityUtil {

    public static final double initMethodSimilarityThreshold = 0.6;
//    public static final double methodSimilarityThreshold = 0.9;
    public static final double methodSimilarityThreshold = 0.6; // for vary evaluate
    public static final double finalMethodSimilarityThreshold = 0.3;

    public static double matchedMethod(MethodAttr lib, MethodAttr app) {
        if (!checkClassHier(lib.declaredClass, app.declaredClass))
            return 0;
        if (!lib.fuzzy.equals(app.fuzzy))
            return 0;
        if (app.callee.isEmpty() && app.fuzzyFieldRef == null)
            return 0;

        double calleeSim = JaccardCall(lib.callee, app.callee);
        double callerSim = JaccardCall(lib.caller, app.caller);
        double fieldSim = JaccardField(lib.fuzzyFieldRef, app.fuzzyFieldRef);
        int fieldCnt = lib.fuzzyFieldRef == null ? 0 : lib.fuzzyFieldRef.size();
        int denominator = lib.callee.size() + lib.caller.size() + fieldCnt;

        return denominator == 0 ? 0 : (calleeSim + callerSim + fieldSim) / denominator;
    }

    public static double matchedInitMethod(MethodAttr lib, MethodAttr app) {
        if (!lib.fuzzy.equals(app.fuzzy))
            return 0;
        double calleeSim = JaccardCall(lib.callee, app.callee);
        double fieldSim = JaccardField(lib.fuzzyFieldRef, app.fuzzyFieldRef);
        int maxField = lib.fuzzyFieldRef == null ? 0 : lib.fuzzyFieldRef.size();
        return (calleeSim + fieldSim) / (lib.callee.size() + maxField);
    }

    private static boolean checkClassHier(ClassAttr lib, ClassAttr app) {
        return (lib.superClasses.size() <= app.superClasses.size());
    }

    private static double JaccardCall(List<MethodAttr> libCalls, List<MethodAttr> appCalls) {
        if (libCalls.size() == 0 || appCalls.size() == 0)
            return 0;
        boolean[] flag = new boolean[appCalls.size()];

        double commonNum = 0;//Number of same elements
        for (MethodAttr libCall : libCalls) {
            for (int i = 0; i < appCalls.size(); i++) {
                if (flag[i])
                    continue;
                if (libCall.fuzzy.equals(appCalls.get(i).fuzzy)) {
                    flag[i] = true;
                    commonNum++;
                    break;
                }
            }
        }
        return commonNum;
    }

    private static double JaccardField(List<String> libFields, List<String> appFields) {
        if (libFields == null || appFields == null)
            return 0;
        boolean[] flag = new boolean[appFields.size()];

        double mergeNum;//Number of union elements
        double commonNum = 0;//Number of same elements
        for (String libField : libFields) {
            for (int i = 0; i < appFields.size(); i++) {
                if (flag[i])
                    continue;
                if (libField.equals(appFields.get(i))) {
                    flag[i] = true;
                    commonNum++;
                    break;
                }
            }
        }
        return commonNum;
    }

    public static boolean isJavaLibraryClass(String str) {
        return str.startsWith("java.")
                || str.startsWith("sun.")
                || str.startsWith("javax.")
                || str.startsWith("com.sun.")
                || str.startsWith("org.omg.")
                || str.startsWith("org.xml.")
                || str.startsWith("org.w3c.dom")
                || str.startsWith("boolean")
                || str.startsWith("char")
                || str.startsWith("byte")
                || str.startsWith("int")
                || str.startsWith("float")
                || str.startsWith("long")
                || str.startsWith("double")
                || str.equals("void")
                || str.equals("static");
    }
}

package similarity;

import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import symbolicExec.MethodDigest;
import treeEditDistance.costmodel.PredicateCostModel;
import treeEditDistance.distance.APTED;
import treeEditDistance.node.Node;
import treeEditDistance.node.PredicateNodeData;

import java.util.List;

public class PathSimilarityThread implements Runnable {
    private final float[] array;
    List<Node<PredicateNodeData>> tplPredicateList;
    Stmt tplLastStmt;
    List<String> tplSignature;
    List<String> tplVariable;
    MethodDigest app;

    public PathSimilarityThread(float[] array, List<Node<PredicateNodeData>> tplPredicateList,
                                List<String> tplSignature, List<String> tplVariable,
                                MethodDigest app, Stmt tplLastStmt) {
        this.array = array;
        this.tplPredicateList = tplPredicateList;
        this.tplSignature = tplSignature;
        this.tplVariable = tplVariable;
        this.app = app;
        this.tplLastStmt = tplLastStmt;
    }

    @Override
    public void run() {
        for (int j = 0; j < app.realUnitsPaths.size(); j++) {
            Stmt appLastStmt = (Stmt) app.realUnitsPaths.get(j).get(app.realUnitsPaths.get(j).size() - 1);
            if ((tplLastStmt instanceof ReturnStmt && appLastStmt instanceof ReturnStmt)
                    || (tplLastStmt instanceof ReturnVoidStmt && appLastStmt instanceof ReturnVoidStmt)
                    || (tplLastStmt instanceof ThrowStmt && appLastStmt instanceof ThrowStmt)) {
                List<Node<PredicateNodeData>> appPredicateList = app.realPredicates.get(j);
                List<String> appSignature = app.realSignatures.get(j);
                List<String> appVariable = app.realVariables.get(j);

                float predicateSim = predicateSimilarityCompute(tplPredicateList, appPredicateList);
                float signatureSim = signatureOrVariableSimilarityCompute_Jaccard(tplSignature, appSignature);
                float variableSim = signatureOrVariableSimilarityCompute(tplVariable, appVariable);

//                predicateSimMatrix[i][j] = predicateSim;
//                signatureSimMatrix[i][j] = signatureSim;
//                variableSimMatrix[i][j] = variableSim;

//                sim[i][j] = (predicateSim * 3 + signatureSim * 2 + variableSim) / 6;
                array[j] = (predicateSim + signatureSim + variableSim) / 3;

            }
        }
    }

    private float predicateSimilarityCompute(List<Node<PredicateNodeData>> tplList,
                                             List<Node<PredicateNodeData>> appList) {
        if (tplList.isEmpty() && appList.isEmpty())
            return 1.0f;

        if (tplList.isEmpty() || appList.isEmpty())
            return 0.0f;

        List<Node<PredicateNodeData>> shorter, longer;
        if (tplList.size() >= appList.size()) {
            shorter = appList;
            longer = tplList;
        } else {
            shorter = tplList;
            longer = appList;
        }

        float[][] predicateSim = new float[shorter.size()][longer.size()];
//        float min = Float.MAX_VALUE;
        for (int i = 0; i < shorter.size(); i++) {
            for (int j = i; j <= longer.size() - shorter.size() + i && j < longer.size(); j++) {
                APTED<PredicateCostModel, PredicateNodeData> apted = new APTED<>(new PredicateCostModel());
                float distance = apted.computeEditDistance_spfTest(shorter.get(i),
                        longer.get(j), 0);
//                if (distance < min)
//                    min = distance;
                predicateSim[i][j] = 1 / (1 + distance / 4);
            }
        }
        return dp.dp_invoke(predicateSim) / Math.max(shorter.size(), longer.size());
    }

    private float signatureOrVariableSimilarityCompute(List<String> tplList, List<String> appList) {
        if (tplList.isEmpty() && appList.isEmpty())
            return 1.0f;
        if (tplList.isEmpty() || appList.isEmpty())
            return 0f;

        List<String> shorter, longer;
        if (tplList.size() >= appList.size()) {
            shorter = appList;
            longer = tplList;
        } else {
            shorter = tplList;
            longer = appList;
        }

        float[][] sigSim = new float[shorter.size()][longer.size()];
        for (int i = 0; i < shorter.size(); i++) {
            for (int j = i; j <= longer.size() - shorter.size() + i && j < longer.size(); j++) {
                sigSim[i][j] = isSignatureMatch(shorter.get(i), longer.get(j));
            }
        }

        return dp.dp_invoke(sigSim) / Math.max(shorter.size(), longer.size());
    }

    private float isSignatureMatch(String sig1, String sig2) {
        String[] split1 = sig1.split(",");
        String[] split2 = sig2.split(",");
        if (split1.length != split2.length)
            return 0f;
        boolean flag = false;// exist "X"
        for (int i = 0; i < split1.length; i++) {
            if (split2[i].startsWith("X") || split1[i].startsWith("X")) {
                flag = true;
                continue;
            }
            if (!split1[i].equals(split2[i]))
                return 0f;
        }
        return flag ? 1.0f : 0.8f;
    }

    private float signatureOrVariableSimilarityCompute_Jaccard(List<String> tplList, List<String> appList) {
        if (tplList.isEmpty() && appList.isEmpty())
            return 1.0f;
        if (tplList.isEmpty() || appList.isEmpty())
            return 0f;
        // simply use Jaccard similarity, otherwise the overhead is tooooo large
        boolean[] libFlag = new boolean[tplList.size()];
        boolean[] appFlag = new boolean[appList.size()];

        float mergeNum;//Number of union elements
        float commonNum = 0;//Number of same elements
        for (int i = 0; i < tplList.size(); i++) {
            if (libFlag[i])
                continue;
            for (int j = 0; j < appList.size(); j++) {
                if (appFlag[j])
                    continue;
                if (isSignatureMatch_Jaccard(tplList.get(i), appList.get(j))) {
                    libFlag[i] = appFlag[j] = true;
                    commonNum++;
                    break;
                }
            }
        }
        mergeNum = tplList.size() + appList.size() - commonNum;
        libFlag = appFlag = null; // free the memory
        return commonNum / mergeNum;
    }

    private boolean isSignatureMatch_Jaccard(String sig1, String sig2) {
        String[] split1 = sig1.split(",");
        String[] split2 = sig2.split(",");
        if (split1.length != split2.length)
            return false;
        for (int i = 0; i < split1.length; i++) {
            if (!split2[i].startsWith("X") && !split1[i].equals(split2[i]))
                return false;
        }
        return true;
    }
}

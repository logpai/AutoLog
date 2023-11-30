package similarity;

import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.ThrowStmt;
import symbolicExec.MethodDigest;

import java.util.List;

public class LibIDThread implements Runnable {
    private final float[] array;
    List<String> tplLibIDPath;
    Stmt tplLastStmt;
    MethodDigest app;

    public LibIDThread(float[] array, List<String> tplLibIDPath, MethodDigest app, Stmt tplLastStmt) {
        this.array = array;
        this.app = app;
        this.tplLibIDPath = tplLibIDPath;
        this.tplLastStmt = tplLastStmt;
    }

    @Override
    public void run() {
        for (int j = 0; j < app.realUnitsPaths.size(); j++) {
            Stmt appLastStmt = (Stmt) app.realUnitsPaths.get(j).get(app.realUnitsPaths.get(j).size() - 1);
            if ((tplLastStmt instanceof ReturnStmt && appLastStmt instanceof ReturnStmt)
                    || (tplLastStmt instanceof ReturnVoidStmt && appLastStmt instanceof ReturnVoidStmt)
                    || (tplLastStmt instanceof ThrowStmt && appLastStmt instanceof ThrowStmt)) {
                List<String> appLibIDPath = app.LibIDPaths.get(j);

                float pathSim = Sim(tplLibIDPath, appLibIDPath);
                array[j] = pathSim;
            }
        }
    }

    private float Sim(List<String> tplList, List<String> appList) {
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
                sigSim[i][j] = 1f / (1f + (float) EditDistance.dp(shorter.get(i), longer.get(i)));
            }
        }
        return dp.dp_invoke(sigSim) / Math.max(shorter.size(), longer.size());
    }
}

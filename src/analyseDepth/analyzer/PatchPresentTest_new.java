package analyseDepth.analyzer;

import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import similarity.CombinationSelect;
import similarity.PathSimilarityThread;
import similarity.SimilarityUtil;
import soot.*;
import soot.jimple.Stmt;
import symbolicExec.MethodDigest;
import treeEditDistance.costmodel.PredicateCostModel_back;
import treeEditDistance.distance.APTED;
import treeEditDistance.node.Node;
import treeEditDistance.node.PredicateNodeData;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static similarity.HungarianAlgorithm.hgAlgorithm;
import static similarity.SimilarityUtil.*;
import static soot.AbstractJasminClass.jasminDescriptorOf;

public class PatchPresentTest_new {
    public static final int maxHandlePath = 1500;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    final Configuration config;
    Map<MarkedMethod, Set<MarkedMethod>> candidateMatchedMethods = new HashMap<>();
    Map<MethodAttr, MarkedMethod> candidateInitMap = new HashMap<>(); // for type recover

    Map<MarkedMethod, MarkedMethod> preOptimalMatchingMap = new HashMap<>();
    Map<MarkedMethod, MarkedMethod> postOptimalMatchingMap = new HashMap<>();

    float preSimilarity = 0;
    float postSimilarity = 0;

    public static ScriptEngine sePy, seJs;

    public PatchPresentTest_new(Configuration config, PatchSummary summary, APKAnalyzer apk) {
        ScriptEngineManager manager = new ScriptEngineManager();
        sePy = manager.getEngineByName("python");
        seJs = manager.getEngineByName("JavaScript");
        this.config = config;
        if (startMatchMethod(summary, apk)) {
//            TimeRecorder.beforeMatching = System.currentTimeMillis();
//            doSimilarityCompute(summary);
//            TimeRecorder.afterMatching = System.currentTimeMillis();
//            computeResult(summary, apk);
        }
    }

    private String getMethodJasminDescriptorOf(SootMethod method) {
        StringBuilder buffer = new StringBuilder();
        buffer.append('(');
        Iterator<Type> var2 = method.getParameterTypes().iterator();

        while (var2.hasNext()) {
            Type t = var2.next();
            buffer.append(jasminDescriptorOf(t));
            if (var2.hasNext()) {
                buffer.append(" ");
            }
        }
        buffer.append(')');
        buffer.append(jasminDescriptorOf(method.getReturnType()));
        return buffer.toString();
    }

    private String getBytecodeSignature(SootMethod method) {
        String buffer = '<' +
                Scene.v().quotedNameOf(method.getDeclaringClass().getName()) +
                ": " +
                method.getName() +
                getMethodJasminDescriptorOf(method) +
                '>';
        return buffer.intern();
    }


    private boolean startMatchMethod(PatchSummary summary, APKAnalyzer apk) {
        for (ClassAttr appClass : apk.allClasses.values()) {
            for (Map.Entry<ClassAttr, Set<MarkedMethod>> classMethodEntry : summary.patchRelatedMethods.entrySet()) {
                Set<MarkedMethod> anchorMethods = classMethodEntry.getValue();
                matchedMethods(anchorMethods, appClass);
            }
        }
        for (Map.Entry<MarkedMethod, Set<MarkedMethod>> entry : candidateMatchedMethods.entrySet()) {
            Set<MarkedMethod> candidates = entry.getValue();

            if (candidates.size() > 1) {
                entry.setValue(new HashSet<>(Ordering.natural().greatestOf(candidates, 1)));
            }
        }

        logger.info("patch-related methods are:");
        for (Map.Entry<ClassAttr, Set<MarkedMethod>> entry : summary.patchRelatedMethods.entrySet()) {
            for (MarkedMethod values : entry.getValue()) {
                Set<MarkedMethod> appMethods = candidateMatchedMethods.get(values);
                if (appMethods == null)
                    continue;
                if (appMethods.isEmpty())
                    continue;
                System.out.print(values.isPre ? "pre" : "post");
                System.out.print("\t" + getBytecodeSignature(values.m.body.getMethod()) + "\t");

                for (MarkedMethod appMethod : appMethods) {
                    System.out.print(getBytecodeSignature(appMethod.m.body.getMethod()) + "\n");
                }

            }
        }

        if (candidateMatchedMethods.isEmpty()) {
            logger.info(String.format("no matched patch-related method in %s, " +
                    "the target method may be deleted by obfuscator.", apk.getAPKName()));
            logger.info("the patch IS NOT PRESENT");

//            if (config.isEnableDebugLevel())
//                logger.info(String.format("cve:%s", summary.getCVENumber()));
            return false;
        }
        return true;
    }

    private void matchedMethods(
            Set<MarkedMethod> anchorMethods,
            ClassAttr appClass) {
        boolean containClinit = false; // exist <clinit>
        boolean containInit = false; // exist <init>
        Map<MarkedMethod, List<MarkedMethod>> tmpMap = new HashMap<>();

        // TODO 如果某个类只改了cinit，有匹配很多很多的类
        for (MarkedMethod anchorMethod : anchorMethods) {
//            if (anchorMethod.isPre && anchorMethod.state == PatchState.Modified)
//                continue;
            for (MethodAttr appMethod : appClass.methods) {
                boolean isAppMethodClinit = appMethod.subSignature.equals("void <clinit>()");
                boolean isAppMethodInit = appMethod.subSignature.contains("<init>");

                if (anchorMethod.m.subSignature.equals("void <clinit>()")
                        != isAppMethodClinit
                        || anchorMethod.m.subSignature.contains("<init>")
                        != isAppMethodInit)
                    continue;
                double sim = SimilarityUtil.matchedMethod(anchorMethod.m, appMethod);
                if (sim > methodSimilarityThreshold) {
                    //TODO check a match <init> is a Sufficient conditions
                    if (!anchorMethod.m.subSignature.contains("<init>")) {
                        Map<MethodAttr, MarkedMethod> pairs = checkConstructMatcher(anchorMethod, appClass);
                        if (!pairs.isEmpty())
                            candidateInitMap.putAll(pairs);
                    }
                    tmpMap.putIfAbsent(anchorMethod, new LinkedList<>());
                    tmpMap.get(anchorMethod).add(new MarkedMethod(sim, appMethod));
                    if (isAppMethodClinit)
                        containClinit = true;
                    if (isAppMethodInit)
                        containInit = true;
                }
            }
        }
        if (((containClinit || containInit) && anchorMethods.size() != 1 && tmpMap.keySet().size() == 1)
                || (containClinit && containInit && anchorMethods.size() != 2 && tmpMap.keySet().size() == 2)) {
            tmpMap = null; // no method in an app class match the target
            return;
        }
//        candidateMatchedMethods.putAll(tmpMap);
        for (Map.Entry<MarkedMethod, List<MarkedMethod>> entry : tmpMap.entrySet()) {
            candidateMatchedMethods.putIfAbsent(entry.getKey(), new HashSet<>());
            candidateMatchedMethods.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    private static Map<MethodAttr, MarkedMethod> checkConstructMatcher(MarkedMethod tpl, ClassAttr apk) {
        Map<MethodAttr, MarkedMethod> pairs = new HashMap<>();
        for (MethodAttr tplInit : tpl.m.declaredClass.methods) {
            if (!tplInit.subSignature.contains("<init>"))
                continue;
            for (MethodAttr apkInit : apk.methods) {
                if (apkInit.subSignature.contains("<init>")) {
                    double sim = matchedInitMethod(tplInit, apkInit);
                    if (sim > initMethodSimilarityThreshold) {
                        if (pairs.containsKey(tplInit)) {
                            MarkedMethod matchedApkInit = pairs.get(tplInit);
                            if (matchedApkInit.sim < sim)
                                pairs.put(tplInit, new MarkedMethod(sim, apkInit));
                        } else
                            pairs.put(tplInit, new MarkedMethod(sim, apkInit));
                        break;
                    }
                }
            }
        }
        return pairs;
    }

    /*
    compute method similarity
     */
    private void doSimilarityCompute(PatchSummary summary) {
        Map<MarkedMethod, MethodDigest> accessedMethod = new HashMap<>();
        for (Set<MarkedMethod> patchRelatedMethods : summary.patchRelatedMethods.values()) {
            for (MarkedMethod patchRelatedMethod : patchRelatedMethods) {
//                System.out.println("1 "+patchRelatedMethod.m.signature);
                MethodDigest tpl = new MethodDigest(patchRelatedMethod.m.body,
                        patchRelatedMethod.patchRelatedLines);
                Set<MarkedMethod> appMethods = candidateMatchedMethods.get(patchRelatedMethod);
                if (appMethods == null)
                    continue;
                float max = -1;
                MarkedMethod optimalPair = null;
                for (MarkedMethod appMethod : appMethods) {
//                    System.out.println("2 "+appMethod.m.signature);
                    MethodDigest app;
                    if (!accessedMethod.containsKey(appMethod)) {
                        app = new MethodDigest(appMethod.m.body, null);
                        Map<String, String> typeRecoveryMap = typeRecovery(patchRelatedMethod, appMethod);
                        recoveryAppMethod(app, typeRecoveryMap);
                        accessedMethod.put(appMethod, app);
                    } else
                        app = accessedMethod.get(appMethod);

                    if (app.realUnitsPaths.isEmpty()) {
                        System.err.println(appMethod.m.signature + " has no paths, that is wired");
                        continue;
                    }

                    // compute the similarity with fine-grain value
                    float sim;

                    sim = MethodSimilarityCompute(tpl, app);
                    if (patchRelatedMethod.isPre) {
                        appMethod.preFineGrainSimilarity = sim;
                    } else {
                        appMethod.postFineGrainSimilarity = sim;
                    }
//                    System.out.println("sim " + sim + " " + patchRelatedMethod.isPre + " " + patchRelatedMethod.m.signature + " " + appMethod.m.signature);
                    if (sim > max) {
                        max = sim;
                        optimalPair = appMethod;
                    }
                }
                if (patchRelatedMethod.isPre)
                    preOptimalMatchingMap.put(patchRelatedMethod, optimalPair);
                else postOptimalMatchingMap.put(patchRelatedMethod, optimalPair);
            }
        }
    }


    private void computeResult(PatchSummary summary,
                               APKAnalyzer apk) {
//        logger.info("candidates methods are:");
//        for (Map.Entry<MarkedMethod, Set<MarkedMethod>> entry : this.candidateMatchedMethods.entrySet()) {
//            MarkedMethod key = entry.getKey();
//            System.out.print(key.isPre + "\t" + key + "\t");
//            for (MarkedMethod value : entry.getValue()) {
//                System.out.print(value.toString() + "#" + value.sim + "#" +
//                        value.preFineGrainSimilarity + "#" + value.postFineGrainSimilarity + "#" + "\t");
//            }
//            System.out.print("\n");
//        }


        // pre - mod + post - mod + mod
        int patchRelatedMethodCnt = summary.postMethodCnt + summary.preMethodCnt - summary.modifiedMethodCnt;

        System.out.printf("patch-related method count = %d\n", patchRelatedMethodCnt);

        double dynamicThreshold = finalMethodSimilarityThreshold / patchRelatedMethodCnt;

        // for pre
        logger.info("The matched pairs for pre-patch TPL and APP are:");
        for (Map.Entry<MarkedMethod, MarkedMethod> entry : preOptimalMatchingMap.entrySet()) {
            MarkedMethod anchorMethod = entry.getKey();
            MarkedMethod appMethod = entry.getValue();
            preSimilarity += appMethod.preFineGrainSimilarity;
//            if (config.enableDebugLevel)
            logger.info(String.format("%s\t%s\t%f", anchorMethod, appMethod, appMethod.preFineGrainSimilarity));
        }
        if (summary.preMethodCnt != 0)
            preSimilarity /= patchRelatedMethodCnt;

        // for post
        logger.info("The matched pairs for post-patch TPL and APP are:");
        for (Map.Entry<MarkedMethod, MarkedMethod> entry : postOptimalMatchingMap.entrySet()) {
            MarkedMethod anchorMethod = entry.getKey();
            MarkedMethod appMethod = entry.getValue();
            postSimilarity += appMethod.postFineGrainSimilarity;
//            if (config.enableDebugLevel)
            logger.info(String.format("%s\t%s\t%f", anchorMethod, appMethod, appMethod.postFineGrainSimilarity));
        }
        if (summary.postMethodCnt != 0)
            postSimilarity /= patchRelatedMethodCnt;

        if (preSimilarity < dynamicThreshold * summary.preMethodCnt
                && postSimilarity < dynamicThreshold * summary.postMethodCnt) {
            logger.info(String.format("all candidate patch-related method similarity are below the threshold ," +
                            "the patch is not present, may be the target method is deleted by the obfuscator \n" +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
            return;
        }
        if (postSimilarity > preSimilarity) {
            logger.info(String.format("the patch IS PRESENT, " +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
        } else if (postSimilarity < preSimilarity) {
            logger.info(String.format("the patch IS NOT PRESENT, " +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
        } else if (Math.abs(postSimilarity - preSimilarity) < 0.0001) {
            logger.info(String.format("the similarity for pre and post is same, that is ward ," +
                            "pre similarity=%f\tpost similarity=%f",
                    preSimilarity, postSimilarity));
        }
//        if (config.isEnableDebugLevel())
//            logger.info(String.format("apk:%s\tcve:%s", apk.getAPKName(), summary.getCVENumber()));
    }

    /*
    argue that matched method between tpl and app should have at least one common <init>
    tpl: A <init>(B b, C c, D d)
    app: A' <init>(B' b, C' c, D' d)

    tpl: X method(Y y, Z z)
    app: X' method(Y' y, Z' z)

    things to be recovered:
    class name, it will influence method, field
    method name
    field name

    when computing similarity, if class or method is from java library or android library, it should be matched 100%
    for those class or method that can not be recovered, we mark it with UNKNOW
     */
    private Map<String, String> typeRecovery(MarkedMethod tplMethod, MarkedMethod appMethod) {
        Map<String, String> tplMapAppClass = new HashMap<>();

        SootMethod tpl = tplMethod.m.body.getMethod();
        SootMethod apk = appMethod.m.body.getMethod();

        // 1. map current class
        tplMapAppClass.put(apk.getDeclaringClass().getName(), tpl.getDeclaringClass().getName());

        // 2. map method sig
        // 2.1 return type
        mapType(tpl.getReturnType(), apk.getReturnType(), tplMapAppClass);

        // 2.2 parameter
        for (int i = 0; i < tpl.getParameterTypes().size(); i++) {
            mapType(tpl.getParameterType(i), apk.getParameterType(i), tplMapAppClass);
        }

        // 3. all possible <init> parameters
        for (MethodAttr tplInit : tplMethod.m.declaredClass.methods) {
            if (!tplInit.subSignature.contains("<init>"))
                continue;
            MarkedMethod matchedAppInitMethod = candidateInitMap.get(tplInit);
            if (matchedAppInitMethod != null) {
                SootMethod tplInitSootMethod = tplInit.body.getMethod();
                SootMethod apkInitSootMethod = matchedAppInitMethod.m.body.getMethod();
                if (apkInitSootMethod == null)
                    continue;
                for (int i = 0; i < tplInitSootMethod.getParameterTypes().size(); i++) {
                    mapType(tplInitSootMethod.getParameterType(i),
                            apkInitSootMethod.getParameterType(i), tplMapAppClass);
                }
            }
        }
        return tplMapAppClass;
    }

    private void mapType(Type tplType, Type appType, Map<String, String> map) {
        if (appType instanceof RefType) {
            RefType refType = (RefType) appType;
            if (refType.getSootClass().isApplicationClass()
                    || refType.getSootClass().isPhantomClass()) {
                StringBuilder tplTypeSb = new StringBuilder();
                if (tplType.toString().contains("[]")) {
                    int index = tplType.toString().indexOf("[");
                    tplTypeSb.append(tplType.toString(), 0, index);
                } else
                    tplTypeSb.append(tplType);

                StringBuilder appTypeSb = new StringBuilder();
                if (refType.toString().contains("[]")) {
                    int index = refType.toString().indexOf("[");
                    appTypeSb.append(refType.toString(), 0, index);
                } else
                    appTypeSb.append(refType);

                map.put(appTypeSb.toString(), tplTypeSb.toString());
            }
        }
    }

    private float MethodSimilarityCompute(MethodDigest tpl, MethodDigest app) {
        int tplPathCount = tpl.realUnitsPaths.size();
        int appPathCount = app.realUnitsPaths.size();

        float[][] sim = new float[tplPathCount][appPathCount];
        ExecutorService es = Executors.newFixedThreadPool(config.getThreadNumber());
        for (int i = 0; i < tplPathCount; i++) {
            Stmt tplLastStmt = (Stmt) tpl.realUnitsPaths.get(i).get(tpl.realUnitsPaths.get(i).size() - 1);
            List<Node<PredicateNodeData>> tplPredicateList = tpl.realPredicates.get(i);
            List<String> tplSignature = tpl.realSignatures.get(i);
            List<String> tplVariable = tpl.realVariables.get(i);
            es.execute(new PathSimilarityThread(sim[i], tplPredicateList, tplSignature,
                    tplVariable, app, tplLastStmt));
//            List<String> tplLibIDPath = tpl.LibIDPaths.get(i);
//            es.execute(new LibIDThread(sim[i], tplLibIDPath, app, tplLastStmt));
        }
        es.shutdown();
        try {
            if (!es.awaitTermination(2, TimeUnit.MINUTES)) {
                es.shutdownNow();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

//        return hgAlgorithm(sim, "max") / Math.max(tplPathCount,appPathCount); // set similarity
        return hgAlgorithm(sim, "max") / tplPathCount; // set similarity
    }

    private void recoveryAppMethod(MethodDigest app, Map<String, String> typeRecoveryMap) {
        // do recovery
        Set<PredicateNodeData> accessd = new HashSet<>();
        for (int i = 0; i < app.realUnitsPaths.size(); i++) {
            List<Node<PredicateNodeData>> appPredicateList = app.realPredicates.get(i);
            List<String> appSignature = app.realSignatures.get(i);
            List<String> appVariable = app.realVariables.get(i);
            for (Node<PredicateNodeData> node : appPredicateList) {
                doRecoveryNode(node, typeRecoveryMap, accessd);
            }

            doRecovery(appSignature, typeRecoveryMap);
            doRecovery(appVariable, typeRecoveryMap);

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
                APTED<PredicateCostModel_back, PredicateNodeData> apted = new APTED<>(new PredicateCostModel_back());
                float distance = apted.computeEditDistance_spfTest(shorter.get(i),
                        longer.get(j), 0);
//                if (distance < min)
//                    min = distance;
                predicateSim[i][j] = 1 / (1 + distance / 4);
            }
        }
//        for (int i = 0; i < shorter.size(); i++) {
//            for (int j = i; j <= longer.size() - shorter.size() + i && j < longer.size(); j++) {
////                predicateSim[i][j] = 1 - predicateSim[i][j] / max;
//                predicateSim[i][j] = predicateSim[i][j] == 0 ? 1 : min / predicateSim[i][j];
//            }
//        }

        CombinationSelect combinationSelect = new CombinationSelect(predicateSim,
                shorter.size(), longer.size());

        predicateSim = null; // free the memory
//        return combinationSelect.max / Math.max(appList.size(), tplList.size()); // in practice, each path in tpl should have a counterpart in app
        return combinationSelect.max / tplList.size(); // in practice, each path in tpl should have a counterpart in app
    }

    private float signatureOrVariableSimilarityCompute(List<String> tplList, List<String> appList) {
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
                if (isSignatureMatch(tplList.get(i), appList.get(j))) {
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


//    private float signatureOrVariableSimilarityCompute(List<String> tplList, List<String> appList) {
//        List<String> shorter, longer;
//        if (tplList.size() >= appList.size()) {
//            shorter = appList;
//            longer = tplList;
//        } else {
//            shorter = tplList;
//            longer = appList;
//        }
//
//        float[][] sim = new float[shorter.size()][longer.size()];
//        float max = -1;
//        for (int i = 0; i < shorter.size(); i++) {
//            for (int j = i; j <= longer.size() - i && j < longer.size(); j++) {
//                sim[i][j] = signatureSimilarity(shorter.get(i), longer.get(j));
//            }
//        }
//
//        CombinationSelect combinationSelect = new CombinationSelect(sim,
//                shorter.size(), longer.size());
//
//        sim = null; // free the memory
//        return combinationSelect.max;
//    }

    private boolean isSignatureMatch(String sig1, String sig2) {
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

    private void doRecoveryNode(Node<PredicateNodeData> node, Map<String, String> typeRecoveryMap,
                                Set<PredicateNodeData> accessed) {
        PredicateNodeData data = node.getNodeData();
        if (accessed.contains(data))
            return;
        accessed.add(data);
        switch (data.getNodeType()) {
            case MultiArray:
            case InstanceOf:
            case Class:
            case Array: // ensure array type is base type
                StringBuilder newSb = new StringBuilder();
                if (!isJavaLibraryClass(data.getData())) {
                    if (data.getData().contains("[]")) {
                        int index = data.getData().indexOf("[");
                        String getFromMap = typeRecoveryMap.getOrDefault(data.getData().substring(0, index), "X");
                        newSb.append(getFromMap).append(data.getData().substring(index));
                    } else
                        newSb.append(typeRecoveryMap.getOrDefault(data.getData(), "X"));
                    data.setData(newSb.toString());
                }
                break;
            case Invoke:
                String[] sigSplit = data.getData().split(",");
                StringBuilder invokeSb = new StringBuilder();
                for (String s : sigSplit) {
                    if (isJavaLibraryClass(s))
                        invokeSb.append(s).append(",");
                    else if (s.contains("[]")) {
                        int index = s.indexOf("[");
                        String getFromMap = typeRecoveryMap.getOrDefault(s.substring(0, index), "X");
                        invokeSb.append(getFromMap).append(s.substring(index)).append(",");
                    } else
                        invokeSb.append(typeRecoveryMap.getOrDefault(s, "X")).append(",");
                }
                data.setData(invokeSb.toString());
                break;
            case Field:
                String[] fieldSplit = data.getData().split("#");
                StringBuilder fieldSb = new StringBuilder();
                for (int i = 0; i < fieldSplit.length; i++) {
                    if (isJavaLibraryClass(fieldSplit[i]))
                        fieldSb.append(fieldSplit[i]);
                    else if (fieldSplit[i].contains("[]")) {
                        int index = fieldSplit[i].indexOf("[");
                        String getFromMap = typeRecoveryMap.getOrDefault(fieldSplit[i].substring(0, index), "X");
                        fieldSb.append(getFromMap).append(fieldSplit[i].substring(index));
                    } else
                        fieldSb.append(typeRecoveryMap.getOrDefault(fieldSplit[i], "X"));
                    if (i == 0)
                        fieldSb.append("#");
                }
                data.setData(fieldSb.toString());
                break;
        }
        for (Node<PredicateNodeData> child : node.getChildren()) {
            doRecoveryNode(child, typeRecoveryMap, accessed);
        }
    }

    private void doRecovery(List<String> appSigList,
                            Map<String, String> typeRecoveryMap) {
        for (int i = 0; i < appSigList.size(); i++) {
            StringBuilder sb = new StringBuilder();
            String[] split = appSigList.get(i).split(",");
            for (String s : split) {
                if (isJavaLibraryClass(s))
                    sb.append(s).append(",");
                else if (s.contains("[]")) {
                    int index = s.indexOf("[");
                    String getFromMap = typeRecoveryMap.getOrDefault(s.substring(0, index), "X");
                    sb.append(getFromMap).append(s.substring(index)).append(",");
                } else
                    sb.append(typeRecoveryMap.getOrDefault(s, "X")).append(",");
            }
            appSigList.set(i, sb.toString());
        }
    }


//    private void report() {
//        try {
//            File out = new File(config.getOutputFile());
//            FileWriter fw = new FileWriter(out);
//            for (Map.Entry<MethodAttr, List<MethodWithSimilarity>> entry : candidateMatchedMethods.entrySet()) {
//                MethodAttr src = entry.getKey();
//                fw.write(src.declaredClass.name + "->" + src.signature + "\n");
//                for (MethodWithSimilarity can : entry.getValue()) {
//                    fw.write(can.sim + " " + can.m.declaredClass.name + "->" + can.m.signature + "#" + can.m.fuzzy + "\n");
//                }
//                fw.write("\n");
//            }
//            fw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}



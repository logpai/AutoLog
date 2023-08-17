package Util;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetInterstedMethods {
    public static ArrayList<String[]> getCallStack(String fileName){
        ArrayList<String[]> callStack = new ArrayList<>();
        File file = new File(fileName);
        BufferedReader reader = null;
        try{
            String tempString = null;
            reader = new BufferedReader(new FileReader(file));
            while((tempString = reader.readLine()) != null){
                callStack.add(tempString.split(" "));
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return callStack;
    }

    public static ArrayList<String> getCallMethods(String fileName){
        ArrayList<String> callStack = new ArrayList<>();
        File file = new File(fileName);
        BufferedReader reader = null;
        try{
            String tempString = null;
            reader = new BufferedReader(new FileReader(file));
            while((tempString = reader.readLine()) != null){
                callStack.add(tempString);
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return callStack;
    }

    private static ArrayList<String> splitClsNameMtdNameParaName(String mtdSig){
        ArrayList<String> mtdInfo = new ArrayList<>();
        String clsName = mtdSig.split(":")[0];
        String mtdName = mtdSig.split(":")[1].split("\\(")[0];
        mtdInfo.add(clsName);
        mtdInfo.add(mtdName);
        String[] para = new String[0];
        String paras = mtdSig.substring(mtdSig.indexOf("(")+1, mtdSig.indexOf(")"));
        if(paras.length() != 0){
            para = paras.split(",");
        }
        mtdInfo.addAll(Arrays.asList(para));
        return mtdInfo;
    }


    public static SootMethod getStartMtd(String mtdSig, Scene scene){

        ArrayList<String> mtdInfo = splitClsNameMtdNameParaName(mtdSig);
        SootClass st = scene.getSootClass(mtdInfo.get(0));
        String mtdName = mtdInfo.get(1);
        ArrayList<String> para = new ArrayList<>();
        if(mtdInfo.size() > 2) {
            if(mtdInfo.size() == 3){
                para.add(mtdInfo.get(2));   //Handle the case when there is only one parameter because sublist(2, -1) will come across with IllegalArgumentException.
            }
            else {
                List<String> paraTmpLst = mtdInfo.subList(2, mtdInfo.size());
                for(String parameter:paraTmpLst) {
                    para.add(parameter);
                }
            }
        }

        SootMethod mtd_null = null;
        List<SootMethod> sootMtds = st.getMethods();
        for(SootMethod mtd:sootMtds){
            if(mtd.getName().equals(mtdName) && mtd.getParameterCount() == para.size()){
                List<Type> paraType = mtd.getParameterTypes();
                if(paraType.size() == 0){
                    return mtd;
                }
                int flag = 1;
                for(int i = 0; i < paraType.size(); i++){
                    if(!paraType.get(i).toString().equals(para.get(i))){
                        flag = 0;
                        break;
                    }
                }
                if(flag == 1){
                    return mtd;
                }

            }
        }
        return mtd_null;
    }
}

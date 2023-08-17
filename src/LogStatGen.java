import Util.*;
import au.com.bytecode.opencsv.CSVWriter;
import com.alibaba.fastjson.JSON;
import org.apache.commons.cli.ParseException;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.util.queue.QueueReader;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;

import java.io.*;
import java.util.*;

public class LogStatGen {
    public static void main(String[] args) throws IOException, ParseException {

        org.apache.commons.cli.Options options = OptionsCfg.LogTempOptions();
        String[] test = args;

        BasicParser basicParser = new BasicParser();
        CommandLine commandLine = basicParser.parse(options, args);

        if (!commandLine.hasOption("jarName")) {
            OptionsCfg.printUsage(options);
            return;
        }

        String classesDir = commandLine.getOptionValue("j");
        String outputFile = commandLine.getOptionValue("o");


        Options.v().set_whole_program(true);
        Options.v().set_app(true);
        Options.v().setPhaseOption("cg", "safe-newinstance:true");
        Options.v().setPhaseOption("cg.cha","enabled:false");

        // Enable SPARK call-graph construction
        Options.v().setPhaseOption("cg.spark","enabled:true");
        Options.v().setPhaseOption("cg.spark","verbose:true");
        Options.v().setPhaseOption("cg.spark","on-fly-cg:true");
        //Options.v().setPhaseOption("cg.spark","merge-stringbuilder:true");
        Options.v().setPhaseOption("cg.spark","string-constants:false");
        Options.v().setPhaseOption("jb","use-original-names:true");

        Options.v().set_allow_phantom_refs(true);

        Options.v().set_soot_classpath(Scene.v().defaultClassPath() + ":../");

        Map<SootMethod, MethodDigest> MethodMap = new HashMap<>();
        PackManager.v().getPack("jtp").add(new Transform("jtp.pre", new CallGraphTransform(MethodMap)));
        //excludeJDKLibrary();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Collections.singletonList(classesDir));
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_ignore_resolving_levels(true);
        Options.v().set_output_format(1);
        Scene.v().loadNecessaryClasses();
        Scene.v().loadBasicClasses();
        PackManager.v().runPacks();


        ArrayList<SootMethod> MethodList = new ArrayList<SootMethod>(MethodMap.keySet());
        ArrayList<LogMethod> listOfMethods = new ArrayList<LogMethod>();
        List<String[]> csvDatas = new ArrayList<>();


        //int counter = 0;
        System.out.println("Start processing the log statement");

        for( SootMethod MyMethod:MethodList) {
            if (true) {
                //counter = counter+1;
                //System.out.println(counter+" out of "+MethodList.size());
                List<String> LogSet = new ArrayList<>();
                List<List<String>> LevelLogPairs = Method2LogString.Method2LogString(MyMethod);
                for (List<String> LogString : LevelLogPairs) {
                    LogSet.add(LogString.get(0));
                    if(LogString.size()>1) {
                        String regex = ".*[a-zA-z].*";
                        if(LogString.get(0).matches(regex)) {
                            csvDatas.add(new String[]{MyMethod.getSignature(), LogString.get(1), LogString.get(0)});
                        }
                    }
                }


                if(!LogSet.isEmpty())
                {
                    List<String> LogList = new ArrayList<>();

                    for (String MyString : LogSet) {
                        LogList.add(MyString);
                    }
                    listOfMethods.add(new LogMethod(MyMethod.getSignature().toString(), LogList));
                }

            }
            else {
                //System.out.println(MyMethod.toString()+" is not in callgraph"+"\n");
            }
        }

        String jarName = classesDir.substring(0,classesDir.length()-4);
        createJsonFile(listOfMethods,jarName+"_logMethods.json");
        createCSVFile(csvDatas, jarName+"_labelling.csv");
        createCSVFile(csvDatas, outputFile);

    }

    public static Map<SootMethod, SootMethod> getAllReachableMethods(SootMethod initialMethod){
        CallGraph callgraph = Scene.v().getCallGraph();
        List<SootMethod> queue = new ArrayList<>();
        queue.add(initialMethod);
        Map<SootMethod, SootMethod> parentMap = new HashMap<>();
        parentMap.put(initialMethod, null);
        for(int i=0; i< queue.size(); i++){
            SootMethod method = queue.get(i);
            for (Iterator<Edge> it = callgraph.edgesOutOf(method); it.hasNext(); ) {
                Edge edge = it.next();
                SootMethod childMethod = edge.tgt();
                if(parentMap.containsKey(childMethod))
                    continue;
                parentMap.put(childMethod, method);
                queue.add(childMethod);
            }
        }
        return parentMap;
    }

    public static String getPossiblePath(Map<SootMethod, SootMethod> reachableParentMap, SootMethod it) {
        String possiblePath = null;
        while(it != null){
            String itName = it.getDeclaringClass().getShortName()+"."+it.getName();
            if(possiblePath == null)
                possiblePath = itName;
            else
                possiblePath = itName + " -> " + possiblePath;
            it = reachableParentMap.get(it);
        } return possiblePath;
    }

    public static boolean createJsonFile(ArrayList<LogMethod> toConvert, String fileName) {
        boolean flag = true;
        String fullPath = fileName;

        try {
            //
            File file = new File(fullPath);
            if (!file.getParentFile().exists()) { //
                file.getParentFile().mkdirs();
            }
            if (file.exists()) { //
                file.delete();
            }
            file.createNewFile();
            String jsonString = JSON.toJSONString(toConvert);

            Writer write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            write.write(jsonString);
            write.flush();
            write.close();
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;
    }


    public  static void createCSVFile(List<String[]> dataList, String finalPath) {
        try {
            File file = new File(finalPath);
            if (!file.getParentFile().exists()) { //
                file.getParentFile().mkdirs();
            }
            if (file.exists()) { //
                file.delete();
            }
            file.createNewFile();

            Writer writer = new FileWriter(finalPath);

            // Bom
            writer.write(new String(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));

            // header
            CSVWriter csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR,
                    CSVWriter.DEFAULT_QUOTE_CHARACTER);
            String[] header = { "Method", "LoggingLevel", "Log" };
            csvWriter.writeNext(header);
            for(String[ ] data:dataList )
            {
                csvWriter.writeNext(data);
            }

            csvWriter.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}


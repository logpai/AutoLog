import Util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import soot.*;
import soot.jimple.parser.node.PCatchClause;
import soot.jimple.parser.node.TCatch;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

import java.io.*;
import java.util.*;

public class LogEPGen
{
    public static void main(String[] args) throws IOException, ParseException {

        org.apache.commons.cli.Options options = OptionsCfg.LogEPOptions();
        BasicParser basicParser = new BasicParser();
        CommandLine commandLine = basicParser.parse(options, args);

        if (!commandLine.hasOption("jarName")) {
            OptionsCfg.printUsage(options);
            return;
        }

        String classesDir = commandLine.getOptionValue("j");
        String filename = commandLine.getOptionValue("l");
        String output = commandLine.getOptionValue("o");


        ArrayList<String> callMethods = GetInterstedMethods.getCallMethods(filename);
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
        //excludeJDKLibrary();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Collections.singletonList(classesDir));
        //Options.v().set_process_dir(classesDir);
        //Options.v().set_soot_classpath(Scene.v().defaultClassPath() + ':'+classesDir);
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_ignore_resolving_levels(true);
        Options.v().set_output_format(1);
        Scene.v().loadNecessaryClasses();
        Scene.v().loadBasicClasses();
        PackManager.v().runPacks();
        System.out.println("Prepare Work Done");

        ArrayList<SootMethod> callRealMethods = new ArrayList<>();
        String jarName = classesDir.substring(0,classesDir.length()-4);

        for( String MyMethod:callMethods) {
            SootMethod MySootMethod = GetInterstedMethods.getStartMtd(MyMethod, Scene.v());
            callRealMethods.add(MySootMethod);
        }

        File jsonfile = new File(output);

        try {
            jsonfile.delete();
            jsonfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileWriter writer = null;

        try {
            Map<String,String> PathMap = new HashMap<String,String>();

            Writer jsonwrite = new OutputStreamWriter(new FileOutputStream(jsonfile), "UTF-8");

            Map<String,String> LogMap = new HashMap<String,String>();
            ArrayList<MethodCalls> toConvert = new ArrayList<>();

            for( String MyMethod:callMethods) {
                try {
                    SootMethod MySootMethod = GetInterstedMethods.getStartMtd(MyMethod, Scene.v());
                    Method2CallPath2 m2c = new Method2CallPath2(MySootMethod, callRealMethods);
                    Set<List<String>> callPaths = m2c.callPaths;
                    Set<String> methodsInLoop = m2c.methodsInLoop;
                    List<List<String>> CallSequences = new ArrayList<>();
                    List<String> CallInLoops = new ArrayList<>();
                    for (List<String> callPath : callPaths) {
                        CallSequences.add(callPath);
                    }

                    for (String loopMethod : methodsInLoop) {
                        CallInLoops.add(loopMethod);
                    }

                    MethodCalls converter = new MethodCalls(MySootMethod.getSignature(), CallSequences, CallInLoops);
                    toConvert.add(converter);
                    String jsonString = JSON.toJSONString(converter, SerializerFeature.DisableCircularReferenceDetect);
                    jsonwrite.write(jsonString+ "\n");
                }
                catch(RuntimeException e)
                {
                    continue;
                }
            }

            writer.close();
            jsonwrite.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static boolean createJsonFile(ArrayList<MethodCalls> toConvert, String fileName) {
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

}


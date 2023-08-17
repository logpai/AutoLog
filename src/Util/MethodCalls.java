package Util;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class MethodCalls {

    @JSONField(name = "Method")
    public String method;

    @JSONField(name = "CallSequences")
    public List<List<String>> CallSequences;

    @JSONField(name = "CallInLoops")
    public List<String> CallInLoops;



    public MethodCalls(String method, List<List<String>> CallSequences,List<String> CallInLoops)
    {
        super();
        this.method = method;
        this.CallSequences = CallSequences;
        this.CallInLoops = CallInLoops;
    }

}

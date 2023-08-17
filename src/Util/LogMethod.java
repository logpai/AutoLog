package Util;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class LogMethod {

    @JSONField(name = "Method")
    public String method;

    @JSONField(name = "LogSequences")
    public List<String> LogSequences;



    public LogMethod(String method, List<String> LogSequences)
    {
        super();
        this.LogSequences = LogSequences;
        this.method = method;
    }

}

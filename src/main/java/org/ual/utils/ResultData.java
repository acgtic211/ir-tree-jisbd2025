package org.ual.utils;

import java.util.HashMap;

public class ResultData {
    public String typeName; // ie. GroupSize
    public String typeValue; // ie. 20
    public HashMap<String, String> cpuResults;
    public HashMap<String, String> ioResults;

    public ResultData() {
        cpuResults = new HashMap<>();
        ioResults = new HashMap<>();
    }

    public ResultData(HashMap<String, String> cpuResults, HashMap<String, String> ioResults) {
        this.cpuResults = cpuResults;
        this.ioResults = ioResults;
    }

    @Override
    public String toString() {
        return "ResultData{" +
                "typeName='" + typeName + '\'' +
                ", typeValue='" + typeValue + '\'' +
                ", cpuResults=" + cpuResults +
                ", ioResults=" + ioResults +
                '}';
    }
}

package com.jerry.trident.join.csdn;

import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Values;

public class FunctionTest extends BaseFunction {  
	  
    @Override  
    public void execute(TridentTuple tuple, TridentCollector collector) {  
        String getName = tuple.getString(0);  
        String getid = tuple.getString(1);  
        String getTel = tuple.getString(2);  
        System.out.println("过滤后数据：============"+tuple.getValues());  
        collector.emit(new Values(getName,getid,getTel));  
    }  
  
}

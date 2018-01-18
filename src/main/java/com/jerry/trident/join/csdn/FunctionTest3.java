package com.jerry.trident.join.csdn;

import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.tuple.TridentTuple;

public class FunctionTest3 extends BaseFunction {  
	  
    @Override  
    public void execute(TridentTuple tuple, TridentCollector collector) {  
          
        System.out.println(tuple.getValues());  
    }  
  
} 
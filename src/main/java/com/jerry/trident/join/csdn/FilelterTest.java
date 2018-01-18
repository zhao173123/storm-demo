package com.jerry.trident.join.csdn;

import org.apache.storm.trident.operation.BaseFilter;
import org.apache.storm.trident.tuple.TridentTuple;

public class FilelterTest  extends BaseFilter{  
	  
    @Override  
    public boolean isKeep(TridentTuple tuple) {  
        String get =tuple.getString(2);  
        if(get.startsWith("186")){  
            return true;  
        }  
        return false;  
    }  
}
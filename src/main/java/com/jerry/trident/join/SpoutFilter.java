package com.jerry.trident.join;

import org.apache.storm.trident.operation.BaseFilter;
import org.apache.storm.trident.tuple.TridentTuple;

public class SpoutFilter extends BaseFilter{

	private static final long serialVersionUID = 2158447746622985530L;

	@Override
	public boolean isKeep(TridentTuple tuple) {
		String get = tuple.getString(2);
		if(get.startsWith("186")){
			return true;
		}
		return false;
	}

	
}

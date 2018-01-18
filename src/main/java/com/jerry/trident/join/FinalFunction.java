package com.jerry.trident.join;

import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.tuple.TridentTuple;

//最终展示
public class FinalFunction extends BaseFunction{

	private static final long serialVersionUID = 1891543143219675882L;

	@Override
	public void execute(TridentTuple tuple, TridentCollector collector) {
		System.out.println(tuple.getValues());
	}

}

package com.jerry.trident.join;

import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Values;

//过滤显示方法
public class SpoutFunction extends BaseFunction{

	private static final long serialVersionUID = -5884581701963963294L;

	@Override
	public void execute(TridentTuple tuple, TridentCollector collector) {
		String getName = tuple.getString(0);
		String getId = tuple.getString(1);
		String getTel = tuple.getString(2);
		System.out.println("过滤后数据：" + tuple.getValues());
		collector.emit(new Values(getName, getId, getTel));
	}

}

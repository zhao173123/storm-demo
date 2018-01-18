package com.jerry.countword.word.bolt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;

/**
 * 报告生成器
 * 
 * @author jerry
 *
 */
public class ReportBolt extends BaseRichBolt{

	private static final long serialVersionUID = 42162486601914032L;

	private HashMap<String, Long> counts = null;
	
	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		this.counts = new HashMap<String, Long>();
	}

	@Override
	public void execute(Tuple input) {
		String word = input.getStringByField("word");
		Long count = input.getLongByField("count");
		this.counts.put(word, count);
		System.out.println("结果 : " + this.counts);
	}

	//末端bolt，不需要发射数据流
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		
	}
	
	/**
	 * IBolt中定义的接口，
	 * Storm在终止一个bolt之前会调用找个方法
	 * 本列：利用cleanup()在Topology关闭时输出最终的结果
	 * 通常情况下，cleanup()用来释放bolt占用的资源，如打开的文件句柄或数据库连接
	 * 但是当Storm拓扑在一个集群上运行时，IBolt.cleanup()不能保证执行
	 * 所以，开发环境可以使用这种方式，集群环境不可以的。
	 */
	public void cleanup(){
		System.out.println("...........Final COUNTS........");
		ArrayList<String> keys = new ArrayList<String>();
		keys.addAll(this.counts.keySet());
		Collections.sort(keys);
		for(String key : keys){
			System.out.println(key + " " + this.counts.get(key));
		}
		System.out.println("---------------------------");
	}

}

package com.jerry.countword.word.bolt;

import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

/**
 * 单词分割器
 * 订阅SentenceSpout发射的tuple流，实现分割单词
 * BasicRichBolt是IComponent和IBolt接口的骨架实现
 * @author jerry
 *
 */
public class SplitSentenceBolt extends BaseRichBolt{

	private static final long serialVersionUID = 8855086247933498129L;

	private OutputCollector collector;

	/**
	 * bolt初始化时调用，准备bolt用到的资源，比如数据库连接等
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
	}

	/**
	 * bolt核心方法，IBolt中定义
	 * 每次Bolt从流中接收一个订阅的tuple都会调用
	 * 本列中，收到的元组中查找“sentence”的值，
	 * 并将该值拆分成单个的词，然后按单词发出新的tuple
	 */
	@Override
	public void execute(Tuple input) {
		String sentence = input.getStringByField("sentence");
		String[] words = sentence.split(" ");
		for(String word : words){
			this.collector.emit(new Values(word));//向下一个bolt发射数据
		}
	}
	
	/**
	 * 输出流，每个包含一个字段（“word”）
	 */
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("word"));
	}

}

package com.jerry.countword.word.bolt;

import java.util.HashMap;
import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

/**
 * 单词计数器
 * 订阅SplitSentenceBolt的输出流，实现单词统计，并发送当前计数给下一个bolt
 * 
 * @author jerry
 *
 */
public class WordCountBolt extends BaseRichBolt{

	private static final long serialVersionUID = 7523476347834101764L;
	
	private OutputCollector collector;
	private HashMap<String, Long> counts = null;//不可序列化对象需要在prepare中实现
	
	/**
	 * 大部分实例变量都需要在prepare中进行实例化，这个设计模式是由Topology的部署方式决定的
	 * 因为在部署拓扑时，组件spout和bolt是在网络上发送的序列化的实例变量
	 * 如果spout或bolt有任何non-serializable实例变量在序列化之前被实例化，
	 * 会抛出NotSerializableException并且拓扑无法发布。
	 * 通常：在构造函数中对基础数据类型和可序列化的对象进行复制和实例化，
	 * 在prepare中对不可序列化的对象进行实例化
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
		this.counts = new HashMap<String, Long>();
	}

	/**
	 * 查找接收到单词计数（如果不存在，初始化为0）
	 * 然后增加计数并存储，发出一个新的词和当前计数器组成的二元组
	 * 发射计数作为流允许拓扑的其他bolt订阅和执行额外的处理
	 */
	@Override
	public void execute(Tuple input) {
		//接收上一个Bolt输出流或者Spout的输入流
		String word = input	.getStringByField("word");
		Long count = this.counts.get(word);
		if(count == null){
			count = 0L;
		}
		count++;
		this.counts.put(word, count);
		this.collector.emit(new Values(word, count));
	}

	/**
	 * 声明一个输出流，其中tuple包括了单词和对应的计数，向后发射
	 * 其他bolt可以订阅找个数据流进一步处理
	 */
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("word", "count"));
	}
}

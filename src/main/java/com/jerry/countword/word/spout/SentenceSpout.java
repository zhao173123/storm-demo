package com.jerry.countword.word.spout;

import java.util.Map;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

/**
 * 数据流生成者
 * 向后端发射tuple数据流
 * 
 * BaseRichSpout是ISoup和IComponent接口的简单实现，
 * 接口对用不到的方法提供了默认的实现（骨架）
 * @author jerry
 *
 */
public class SentenceSpout extends BaseRichSpout {

	private static final long serialVersionUID = 7730503843347162546L;
	
	private SpoutOutputCollector collector; 
	private String[] sentences = {
			"my name is soul",
			"i'm a boy",
			"i have a dog",
			"my dog has fleas",
			"my girl friend is beautiful"
	};
	private int index = 0;
	
	/**
	 * open()是ISpout中的接口，在Spout组件初始化时被调用
	 * 
	 * Map：storm配置的map
	 * TopologyContext：提供TopologyContext中组件的信息
	 * SpoutOutputCollector：SpoutOutputCollector对象提供发射Tuple的方法
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		this.collector = collector; 
	}
	
	/**
	 * 核心方法
	 * Storm调用此方法，向输出的collector发出tuple
	 * 这里：发射当前索引的句子，并增加索引准备发射下一个句子
	 */
	@Override
	public void nextTuple() {
		this.collector.emit(new Values(sentences[index]));
		index++;
		if(index >= sentences.length){
			index = 0;
		}
		Utils.sleep(1);
	}

	/**
	 * IComponent接口定义，所有storm组件（spout、bolt）都必须实现这个接口
	 * 用于告诉storm流组件将会发出哪些数据，每个流的tuple将包含的字段
	 */
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("sentence"));//告诉组件发出数据流包含sentence字段
	}

}

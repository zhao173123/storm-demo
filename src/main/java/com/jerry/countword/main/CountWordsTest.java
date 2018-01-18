package com.jerry.countword.main;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.tuple.Fields;
import org.apache.storm.utils.Utils;

import com.jerry.countword.word.bolt.ReportBolt;
import com.jerry.countword.word.bolt.SplitSentenceBolt;
import com.jerry.countword.word.bolt.WordCountBolt;
import com.jerry.countword.word.spout.SentenceSpout;

/**
 * 单词统计入口
 * 实现单词统计topology
 * 
 * @author jerry
 *
 */
public class CountWordsTest {

	private static final String SENTENCE_SPOUT_ID = "sentence-spout";
	private static final String SPLIT_BOLT_ID = "split-bolt";
	private static final String COUNT_BOLT_ID = "count-bolt";
	private static final String REPORT_BOLT_ID = "report-bolt";
	private static final String TOPOLOGY_NAME = "word-count-topology";
	
	public static void main(String[] args){
		SentenceSpout spout = new SentenceSpout();
		SplitSentenceBolt splitBolt = new SplitSentenceBolt();
		WordCountBolt countBolt = new WordCountBolt();
		ReportBolt reportBolt = new ReportBolt();
		
		//TopologyBuilder提供流式API定义topology组件之间的数据流
		TopologyBuilder builder = new TopologyBuilder();
		//设置Executor（线程），默认为1个
		//注册一个sentence spout
		builder.setSpout(SENTENCE_SPOUT_ID, spout, 2);
		
		// SentenceSpout --> SplitSentenceBolt
		builder.setBolt(SPLIT_BOLT_ID, splitBolt, 2) //注册一个splitBolt并订阅sentence发射出的数据流
				.setNumTasks(4)// SplitSentenceBolt单词分割器设置4个Task，2个Executor
				.shuffleGrouping(SENTENCE_SPOUT_ID);// 将SentenceSpout发射的Tuple随机均匀的分发给SplitSentenceBolt实例
		// SplitSentenceBolt --> WordCountBolt
		builder.setBolt(COUNT_BOLT_ID, countBolt) // 注册一个countBolt并订阅splitBolt发射的数据流
				.fieldsGrouping(SPLIT_BOLT_ID, new Fields("word"));//将含有特定数据的tuple路由到特殊的bolt实例中
		// WordCountBolt --> ReportBolt
		builder.setBolt(REPORT_BOLT_ID, reportBolt)// 注册一个reportBolt
				.globalGrouping(COUNT_BOLT_ID); // 将WordCountBolt发射的所有tuple路由到唯一的ReportBolt
		
		//Config是HashMap的一个子类，用来配置topology运行时的行为
		Config config = new Config();
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
		
		Utils.sleep(10000);
		cluster.killTopology(TOPOLOGY_NAME);
		cluster.shutdown();
	}
}

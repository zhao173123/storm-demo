package com.jerry.trident.join;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.trident.Stream;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.testing.FixedBatchSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

/**
 * 流的合并操作
 * 需求：
 * 两个Spout，spout1里面的数据是[name,id,tel]
 * spout2里面的数据是[sex,id]
 * 首先对spout1进行过滤，过滤掉不是186的电话号码并显示
 * 然后根据过滤后的stream和spout2进行合并
 * 
 * @author jerry
 *
 */
public class TestTridentJoin {

	@SuppressWarnings("unchecked")
	private static FixedBatchSpout spout = new FixedBatchSpout(new Fields("name", "idName", "tel"), 3, 
			new Values("Jack", "1", "186017"), new Values("Tome", "2", "1514697"), 
			new Values("Lay", "3", "186745"), new Values("Lucy", "4", "1396478"));
	@SuppressWarnings("unchecked")
	static FixedBatchSpout spout2 = new FixedBatchSpout(new Fields("sex", "idSex"), 3,
			new Values("Boy", "1"), new Values("Boy", "2"), 
			new Values("Gril", "3"), new Values("Gril", "4"));
	
	public static void main(String[] args){
		//设置是否循环
		spout.setCycle(false);
		//构建TridentTopology
		TridentTopology topology = new TridentTopology();
		//过滤tel
		SpoutFilter spoutFilter = new SpoutFilter();
		//显示过滤后的sput数据流
		SpoutFunction spoutFunction = new SpoutFunction();
		//根据spout构建一个stream流
		Stream st = topology.newStream("sp1", spout);
		//对第一个Stream数据显示
		Stream st_1 = st.each(new Fields("name", "idName", "tel"), 
				spoutFunction, 
				new Fields("out_name", "out_idName", "out_tel"));
		//根据第二个Spout构建Stream
		Stream st_2 = topology.newStream("sp2", spout2);
		
		Stream st_3 = topology.join(st, new Fields("idName"), st_2, new Fields("idSex"),
				new Fields("Res_id", "Res_name", "Res_tel", "Res_sex"));
		//显示合并后的结果集
		FinalFunction finalFunction = new FinalFunction();
		st_3.each(new Fields("Res_id", "Res_name", "Res_tel", "Res_sex"), spoutFilter)
			.each(new Fields("Res_id", "Res_name", "Res_tel", "Res_sex"), finalFunction, 
					new Fields("out1_id", "out1_name", "out1_tel", "out1_sex"));
		
		Config conf = new Config();
//		conf.setNumWorkers(2);
//		conf.setNumAckers(0);
//		conf.setDebug(false);
		
//		LocalCluster lc = new LocalCluster();
//		lc.submitTopology("TestTopo", conf, topology.build());
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("TestTopo", conf, topology.build());
		
		Utils.sleep(10000);
		cluster.killTopology("TestTopo");
		cluster.shutdown();
	}
}

package com.jerry.trident.myself;

import org.apache.storm.Config;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.builtin.Count;
import org.apache.storm.trident.testing.FixedBatchSpout;
import org.apache.storm.trident.testing.MemoryMapState;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;


public class Demo {

	@SuppressWarnings("unchecked")
	private static FixedBatchSpout spout = new FixedBatchSpout(new Fields("sentence"), 3, 
			new Values("the cow jumped over the moon"), 
			new Values("the man went to the store and bought some candy"), 
			new Values("four score and seven years ago"),
			new Values("how many apples can you eat"));
	
	public static void main(String[] args){
		TridentTopology topology = new TridentTopology();
		topology.newStream("spout1", spout)
				.each(new Fields("sentence"), new Split(), new Fields("word"))
				.groupBy(new Fields("word"))
				.persistentAggregate(new MemoryMapState.Factory(), new Count(), new Fields("count"))
				.parallelismHint(6);
		
		
	}
}

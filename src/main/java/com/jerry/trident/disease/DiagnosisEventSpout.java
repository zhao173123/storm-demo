package com.jerry.trident.disease;

import java.util.Map;

import org.apache.storm.task.TopologyContext;
import org.apache.storm.trident.spout.BatchSpoutExecutor.EmptyCoordinator;
import org.apache.storm.trident.spout.ITridentSpout;
import org.apache.storm.tuple.Fields;

public class DiagnosisEventSpout implements ITridentSpout<Long>{

	private static final long serialVersionUID = 1L;
	
	BatchCoordinator<Long> coordinator = new EmptyCoordinator();
	
	@Override
	public BatchCoordinator<Long> getCoordinator(String txStateId,
			Map conf, TopologyContext context) {
		
		return null;
	}

	@Override
	public Emitter<Long> getEmitter(String txStateId, Map conf,
			TopologyContext context) {
		return null;
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		return null;
	}

	@Override
	public Fields getOutputFields() {
		return null;
	}

}

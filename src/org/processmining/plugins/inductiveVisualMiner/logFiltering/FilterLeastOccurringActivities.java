package org.processmining.plugins.inductiveVisualMiner.logFiltering;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.dfgOnly.log2logInfo.IMLog2IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.Miner;
import org.processmining.plugins.InductiveMiner.mining.MinerState;
import org.processmining.plugins.InductiveMiner.mining.cuts.Cut;
import org.processmining.plugins.InductiveMiner.mining.cuts.Cut.Operator;
import org.processmining.plugins.InductiveMiner.mining.logSplitter.LogSplitter.LogSplitResult;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog2;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.miners.MiningParametersIvM;

public class FilterLeastOccurringActivities {
	public static Triple<IMLog2, IMLogInfo, Set<XEventClass>> filter(IMLog2 log, IMLogInfo logInfo, double threshold,
			IMLog2IMLogInfo log2logInfo) {
		List<XEventClass> list = logInfo.getActivities().sortByCardinality();
		int lastIndex = (int) Math.floor((1 - threshold) * list.size());

		//make a cut to filter
		Set<XEventClass> remove = new HashSet<XEventClass>(list.subList(0, lastIndex));
		Set<XEventClass> keep = new HashSet<XEventClass>(list.subList(lastIndex, list.size()));
		List<Set<XEventClass>> partition = new ArrayList<Set<XEventClass>>();
		partition.add(keep);
		partition.add(remove);
		Cut cut = new Cut(Operator.parallel, partition);
		MinerState minerState = new MinerState(new MiningParametersIvM());
		LogSplitResult result = Miner.splitLog(log, logInfo, cut, minerState);
		
		IMLogInfo filteredLogInfo = log2logInfo.createLogInfo(result.sublogs.get(0));
		
		return new Triple<IMLog2, IMLogInfo, Set<XEventClass>>(result.sublogs.get(0), filteredLogInfo, remove);
	}
}

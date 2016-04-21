package org.processmining.plugins.inductiveVisualMiner.chain;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Sextuple;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignmentPerformance;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogInfo;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;
import org.processmining.plugins.inductiveVisualMiner.performance.XEventPerformanceClassifier;
import org.processmining.processtree.ProcessTree;

public class Cl05Align
		extends
		ChainLink<Sextuple<ProcessTree, XEventPerformanceClassifier, XLog, IMLog, XEventClasses, XEventClasses>, Pair<IvMLogNotFiltered, IvMLogInfo>> {

	protected Sextuple<ProcessTree, XEventPerformanceClassifier, XLog, IMLog, XEventClasses, XEventClasses> generateInput(
			InductiveVisualMinerState state) {
		return Sextuple.of(state.getTree(), state.getPerformanceClassifier(), state.getXLog(), state.getLog(), state
				.getXLogInfo().getEventClasses(), state.getXLogInfoPerformance().getEventClasses());
	}

	protected Pair<IvMLogNotFiltered, IvMLogInfo> executeLink(
			Sextuple<ProcessTree, XEventPerformanceClassifier, XLog, IMLog, XEventClasses, XEventClasses> input,
			IvMCanceller canceller) throws Exception {
		IvMLogNotFiltered log = AlignmentPerformance.align(input.getA(), input.getB(), input.getC(), input.getD(),
				input.getE(), input.getF(), canceller);
		if (log == null) {
			return null;
		}
		IvMLogInfo logInfo = new IvMLogInfo(log);
		return Pair.of(log, logInfo);
	}

	protected void processResult(Pair<IvMLogNotFiltered, IvMLogInfo> result, InductiveVisualMinerState state) {
		state.setIvMLog(result.getA(), result.getB());
	}

}
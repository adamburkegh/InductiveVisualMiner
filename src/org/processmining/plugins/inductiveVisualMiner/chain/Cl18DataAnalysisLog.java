package org.processmining.plugins.inductiveVisualMiner.chain;

import java.util.List;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.logattributes.LogAttributeAnalysis;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFiltered;

/**
 * Most of the work for the log attribute analysis is done in Cl02SortEvents.
 * This class only adds a few measures to the log attributes.
 * 
 * @author sander
 *
 */
public class Cl18DataAnalysisLog extends ChainLink<IvMLogFiltered, List<Pair<String, DisplayType>>> {

	public String getName() {
		return "data analysis - log";
	}

	protected IvMLogFiltered generateInput(InductiveVisualMinerState state) {
		return state.getIvMLogFiltered();
	}

	protected List<Pair<String, DisplayType>> executeLink(IvMLogFiltered input, IvMCanceller canceller)
			throws Exception {
		return LogAttributeAnalysis.computeVirtualAttributes(input, canceller);
	}

	protected void processResult(List<Pair<String, DisplayType>> result, InductiveVisualMinerState state) {
		LogAttributeAnalysis attributes = state.getLogAttributesAnalysis();
		attributes.addVirtualAttributes(result);
	}

	protected void invalidateResult(InductiveVisualMinerState state) {
		LogAttributeAnalysis attributes = state.getLogAttributesAnalysis();
		if (attributes != null) {
			attributes.setVirtualAttributesToPlaceholders();
		}
	}

	public String getStatusBusyMessage() {
		return "Performing log analysis..";
	}

}
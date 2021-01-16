package org.processmining.plugins.inductiveVisualMiner.chain;

import java.util.List;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.configuration.InductiveVisualMinerConfiguration;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.logattributes.LogAttributeAnalysis;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFilteredImpl;

/**
 * Most of the work for the log attribute analysis is done in Cl02SortEvents.
 * This class only adds a few measures to the log attributes.
 * 
 * @author sander
 *
 */
public class Cl19DataAnalysisLog implements DataChainLinkComputation {

	@Override
	public String getName() {
		return "data analysis - log";
	}

	@Override
	public String getStatusBusyMessage() {
		return "Performing log analysis..";
	}

	@Override
	public IvMObject<?>[] getInputNames() {
		return new IvMObject<?>[] { IvMObject.model, IvMObject.aligned_log_filtered, IvMObject.data_analysis_log };
	}

	@Override
	public IvMObject<?>[] getOutputNames() {
		return new IvMObject<?>[] { IvMObject.data_analysis_log_virtual_attributes };
	}

	@Override
	public IvMObjectValues execute(InductiveVisualMinerConfiguration configuration, IvMObjectValues inputs,
			IvMCanceller canceller) throws Exception {
		IvMModel model = inputs.get(IvMObject.model);
		IvMLogFilteredImpl log = inputs.get(IvMObject.aligned_log_filtered);
		LogAttributeAnalysis dataAnalysis = inputs.get(IvMObject.data_analysis_log);

		List<Pair<String, DisplayType>> virtualAttributes = LogAttributeAnalysis.computeVirtualAttributes(model, log,
				canceller);

		return new IvMObjectValues().//
				s(IvMObject.data_analysis_log_virtual_attributes, virtualAttributes);
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

}
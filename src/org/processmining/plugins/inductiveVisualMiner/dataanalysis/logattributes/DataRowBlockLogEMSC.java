package org.processmining.plugins.inductiveVisualMiner.dataanalysis.logattributes;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.model.XLog;
import org.processmining.earthmoversstochasticconformancechecking.parameters.EMSCParametersLogLogAbstract;
import org.processmining.earthmoversstochasticconformancechecking.parameters.EMSCParametersLogLogDefault;
import org.processmining.earthmoversstochasticconformancechecking.plugins.EarthMoversStochasticConformancePlugin;
import org.processmining.earthmoversstochasticconformancechecking.tracealignments.StochasticTraceAlignmentsLogLog;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMCanceller;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObject;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObjectValues;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRow;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRowBlockComputer;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DisplayType;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLog2XLog;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogFilteredImpl;

public class DataRowBlockLogEMSC<C, P> extends DataRowBlockComputer<C, P> {

	public String getName() {
		return "log-att-emsc";
	}

	public IvMObject<?>[] createInputObjects() {
		return new IvMObject<?>[] { IvMObject.aligned_log_filtered, IvMObject.model };
	}

	public List<DataRow> compute(C configuration, IvMObjectValues inputs, IvMCanceller canceller) throws Exception {
		IvMLogFilteredImpl logFiltered = inputs.get(IvMObject.aligned_log_filtered);
		IvMModel model = inputs.get(IvMObject.model);

		List<DataRow> result = new ArrayList<>();

		if (logFiltered.isSomethingFiltered()) {
			IvMLogFilteredImpl negativeLog = logFiltered.clone();
			negativeLog.invert();

			//transform to xlog
			XLog logA = IvMLog2XLog.convert(logFiltered, model);
			XLog logB = IvMLog2XLog.convert(negativeLog, model);

			EMSCParametersLogLogAbstract parameters = new EMSCParametersLogLogDefault();
			parameters.setComputeStochasticTraceAlignments(false);
			StochasticTraceAlignmentsLogLog alignments = EarthMoversStochasticConformancePlugin.measureLogLog(logA,
					logB, parameters, canceller);

			if (canceller.isCancelled()) {
				return null;
			}

			DisplayType zz = DisplayType.numeric(alignments.getSimilarity());
			result.add(new DataRow(zz, "stochastic similarity between highlighted and non-highlighted traces"));
		}

		return result;
	}
}

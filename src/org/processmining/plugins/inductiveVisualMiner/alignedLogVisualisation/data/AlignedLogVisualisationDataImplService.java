package org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data;

import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogInfo;
import org.processmining.plugins.inductiveVisualMiner.performance.Performance;
import org.processmining.plugins.inductiveVisualMiner.performance.PerformanceWrapper;
import org.processmining.plugins.inductiveVisualMiner.performance.PerformanceWrapper.Gather;
import org.processmining.plugins.inductiveVisualMiner.performance.PerformanceWrapper.Type;

public class AlignedLogVisualisationDataImplService extends AlignedLogVisualisationDataImplSojourn {

	public AlignedLogVisualisationDataImplService(IvMModel model, PerformanceWrapper performance, IvMLogInfo logInfo) {
		super(model, performance, logInfo);
	}

	@Override
	protected void computeExtremes(PerformanceWrapper performance) {
		//compute extreme average times
		minMeasure = Long.MAX_VALUE;
		maxMeasure = Long.MIN_VALUE;
		for (long d : performance.getMeasures(Type.service, Gather.average)) {
			if (d > maxMeasure) {
				maxMeasure = d;
			}
			if (d < minMeasure) {
				minMeasure = d;
			}
		}
	}

	@Override
	public Triple<String, Long, Long> getNodeLabel(int unode, boolean includeModelMoves) {
		long length = performance.getMeasure(Type.service, Gather.average, unode);
		if (length > -1) {
			return Triple.of(Performance.timeToString(length), length, length);
		} else {
			return Triple.of("-", -1l, -1l);
		}
	}

}

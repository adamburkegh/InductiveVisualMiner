package org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data;

import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogInfo;
import org.processmining.plugins.inductiveVisualMiner.performance.Performance;
import org.processmining.plugins.inductiveVisualMiner.performance.PerformanceWrapper;

public class AlignedLogVisualisationDataImplService extends AlignedLogVisualisationDataImplSojourn {

	public AlignedLogVisualisationDataImplService(IvMModel model, PerformanceWrapper queueLengths, IvMLogInfo logInfo) {
		super(model, queueLengths, logInfo);
	}

	@Override
	protected void computeExtremes(PerformanceWrapper queueLengths) {
		//compute extreme sojourn times
		minQueueLength = Long.MAX_VALUE;
		maxQueueLength = Long.MIN_VALUE;
		for (double d : queueLengths.getServiceTimes().values()) {
			if (d > maxQueueLength) {
				maxQueueLength = Math.round(d);
			}
			if (d < minQueueLength) {
				minQueueLength = Math.round(d);
			}
		}
	}

	@Override
	public Triple<String, Long, Long> getNodeLabel(int unode, boolean includeModelMoves) {
		long length = Math.round(queueLengths.getServiceTime(unode));
		if (length >= 0) {
			return Triple.of(Performance.timeToString(length), length, length);
		} else {
			return Triple.of("-", -1l, -1l);
		}
	}

}

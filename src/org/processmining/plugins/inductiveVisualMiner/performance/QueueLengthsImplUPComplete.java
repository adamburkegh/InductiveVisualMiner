package org.processmining.plugins.inductiveVisualMiner.performance;

import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;

public class QueueLengthsImplUPComplete extends QueueLengths {

	public double getQueueProbability(UnfoldedNode unode, QueueActivityLog l, long time, int traceIndex) {
		if (l.getInitiate(traceIndex) > 0 && l.getComplete(traceIndex) > 0 && l.getInitiate(traceIndex) <= time
				&& time <= l.getComplete(traceIndex)) {
			return 1/3.0;
		}
		return 0;
	}

}

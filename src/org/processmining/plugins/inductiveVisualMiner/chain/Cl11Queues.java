package org.processmining.plugins.inductiveVisualMiner.chain;

import java.util.Map;

import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.animation.IvMLog;
import org.processmining.plugins.inductiveVisualMiner.performance.QueueActivityLog;
import org.processmining.plugins.inductiveVisualMiner.performance.QueueLengths;
import org.processmining.plugins.inductiveVisualMiner.performance.QueueLengthsImplCombination;
import org.processmining.plugins.inductiveVisualMiner.performance.QueueLengthsWrapper;
import org.processmining.plugins.inductiveVisualMiner.performance.QueueMineActivityLog;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;

public class Cl11Queues extends ChainLink<IvMLog, QueueLengthsWrapper> {

	protected IvMLog generateInput(InductiveVisualMinerState state) {
		if (state.getColourMode().isShowQueueLengths()) {
			return state.getIvMLog();
		} else {
			return null;
		}
	}

	protected QueueLengthsWrapper executeLink(IvMLog input) {
		if (input != null) {
			Map<UnfoldedNode, QueueActivityLog> queueActivityLogs = QueueMineActivityLog.mine(input);
	
			QueueLengths method = new QueueLengthsImplCombination(queueActivityLogs);
			return new QueueLengthsWrapper(method, queueActivityLogs);
		} else {
			return null;
		}
	}

	protected void processResult(QueueLengthsWrapper result, InductiveVisualMinerState state) {
		state.setQueueLengths(result);
	}

	public void cancel() {

	}
}

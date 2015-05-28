package org.processmining.plugins.inductiveVisualMiner.performance;

import java.util.Map;

import javax.swing.JOptionPane;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.processmining.plugins.inductiveVisualMiner.animation.IvMLog;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;

public class QueueLengthsImplPHComplete implements QueueLengths {

	private class Cluster implements Comparable<Cluster> {
		public int size;
		public double center;

		public int compareTo(Cluster arg0) {
			return Double.compare(center, arg0.center);
		}
	}

	private final Map<UnfoldedNode, QueueActivityLog> queueActivityLogs;

	private final double lambda1;
	private final double lambda2;
	private final double lambda3;

	public QueueLengthsImplPHComplete(IvMLog iLog) {
		queueActivityLogs = QueueMineActivityLog.mine(iLog, true, false, false, true);
		
		lambda1 = Double.valueOf((String) JOptionPane.showInputDialog(null, "Lambda", "Lambda 1",
				JOptionPane.PLAIN_MESSAGE, null, null, ""));
		lambda2 = Double.valueOf((String) JOptionPane.showInputDialog(null, "Lambda", "Lambda 2",
				JOptionPane.PLAIN_MESSAGE, null, null, ""));
		lambda3 = Double.valueOf((String) JOptionPane.showInputDialog(null, "Lambda", "Lambda 3",
				JOptionPane.PLAIN_MESSAGE, null, null, ""));
	}

	public double getQueueLength(UnfoldedNode unode, long time) {
		QueueActivityLog l = queueActivityLogs.get(unode);
		if (l == null) {
			return -1;
		}
		
		double queueLength = 0;
		for (int index = 0; index < l.size(); index++) {
			if (l.getInitiate(index) <= time && time <= l.getComplete(index)) {

				long xI = time - l.getInitiate(index);
		
				DoubleMatrix m = DoubleMatrix.zeros(3, 3);
				m.put(0, 0, (-lambda1) * xI);
				m.put(0, 1, lambda1 * xI);
				m.put(1,  1, (-lambda2) * xI);
				m.put(1, 2, lambda2 * xI);
				m.put(2, 2, (-lambda3) * xI);
				DoubleMatrix m2 = MatrixFunctions.expm(m);
				
				double p = m2.get(0, 1);
				
				queueLength += p;
			}
		}
		return queueLength;
	}
}

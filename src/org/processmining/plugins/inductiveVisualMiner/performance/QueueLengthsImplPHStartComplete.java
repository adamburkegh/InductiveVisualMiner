package org.processmining.plugins.inductiveVisualMiner.performance;

import javax.swing.JOptionPane;

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;

public class QueueLengthsImplPHStartComplete extends QueueLengths {

	private final double lambda1;
	private final double lambda2;

	public QueueLengthsImplPHStartComplete() {
		lambda1 = Double.valueOf((String) JOptionPane.showInputDialog(null, "Lambda", "Lambda 1",
				JOptionPane.PLAIN_MESSAGE, null, null, ""));
		lambda2 = Double.valueOf((String) JOptionPane.showInputDialog(null, "Lambda", "Lambda 2",
				JOptionPane.PLAIN_MESSAGE, null, null, ""));
	}

	public double getQueueProbability(UnfoldedNode unode, QueueActivityLog l, long time, int traceIndex) {
		if (l.getInitiate(traceIndex) > 0 && l.getComplete(traceIndex) > 0 && l.getInitiate(traceIndex) <= time
				&& time <= l.getComplete(traceIndex)) {
			long xI = time - l.getInitiate(traceIndex);

			DoubleMatrix m = DoubleMatrix.zeros(2, 2);
			m.put(0, 0, (-lambda1) * xI);
			m.put(0, 1, lambda1 * xI);
			m.put(1, 1, (-lambda2) * xI);
			DoubleMatrix m2 = MatrixFunctions.expm(m);

			return m2.get(0, 1);
		}
		return 0;
	}
}

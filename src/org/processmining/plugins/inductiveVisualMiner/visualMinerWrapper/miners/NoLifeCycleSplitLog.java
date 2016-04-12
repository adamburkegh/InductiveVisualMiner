package org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.miners;

import org.processmining.framework.packages.PackageManager.Canceller;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.VisualMinerParameters;
import org.processmining.plugins.inductiveVisualMiner.visualMinerWrapper.VisualMinerWrapper;
import org.processmining.processtree.ProcessTree;

public class NoLifeCycleSplitLog extends VisualMinerWrapper {

	public String toString() {
		return "without life cycle";
	}

	public ProcessTree mine(IMLog log, VisualMinerParameters parameters, final Canceller canceller) {
		
		//copy the relevant parameters
		MiningParameters miningParameters = new NoLifeCycleMiningParametersIvM();
		miningParameters.setNoiseThreshold((float) (1 - parameters.getPaths()));
		
		return IMProcessTree.mineProcessTree(log, miningParameters, canceller);
	}
	
}

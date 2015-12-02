package org.processmining.plugins.inductiveVisualMiner.chain;

import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.animation.ComputeAnimation;
import org.processmining.plugins.inductiveVisualMiner.animation.Scaler;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLog;

public class Cl07AnimationScaler extends ChainLink<IvMLog, Scaler> {

	public Cl07AnimationScaler(ProMCanceller globalCanceller) {
		super(globalCanceller);
	}

	protected IvMLog generateInput(InductiveVisualMinerState state) {
		return state.getIvMLog();
	}

	protected Scaler executeLink(IvMLog input) throws Exception {
		Scaler scaler = Scaler.fromLog(input, ComputeAnimation.initDuration, ComputeAnimation.animationDuration,
				canceller);
		if (scaler == null) {
			scaler = Scaler.fromValues(ComputeAnimation.animationDuration);
		}
		return scaler;
	}

	protected void processResult(Scaler result, InductiveVisualMinerState state) {
		state.setAnimationScaler(result);
	}

}

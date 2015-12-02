package org.processmining.plugins.inductiveVisualMiner.chain;

import java.util.concurrent.Executor;

import javax.swing.SwingUtilities;

import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.plugins.InductiveMiner.Function;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Quintuple;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerPanel;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.animation.ComputeAnimation;
import org.processmining.plugins.inductiveVisualMiner.animation.GraphVizTokens;
import org.processmining.plugins.inductiveVisualMiner.animation.Scaler;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.InputFunction;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.ThreadedComputer;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLog;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;
import org.processmining.plugins.inductiveVisualMiner.mode.Mode;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationInfo;

import com.kitfox.svg.SVGDiagram;

public class Cl08Animate extends ChainLink<Double, Double> {

	private final ProMCanceller globalCanceller;

	private final ThreadedComputer<Quintuple<IvMLogNotFiltered, Mode, ProcessTreeVisualisationInfo, SVGDiagram, Scaler>, GraphVizTokens> pool;

	public Cl08Animate(final Executor executor, final InductiveVisualMinerState state,
			final InductiveVisualMinerPanel panel, final ProMCanceller canceller) {
		super(canceller);
		this.globalCanceller = canceller;

		pool = new ThreadedComputer<Quintuple<IvMLogNotFiltered, Mode, ProcessTreeVisualisationInfo, SVGDiagram, Scaler>, GraphVizTokens>(
				executor,
				new Function<Pair<ResettableCanceller, Quintuple<IvMLogNotFiltered, Mode, ProcessTreeVisualisationInfo, SVGDiagram, Scaler>>, GraphVizTokens>() {

					//this function performs the computation
					public GraphVizTokens call(
							Pair<org.processmining.plugins.inductiveVisualMiner.chain.ChainLink.ResettableCanceller, Quintuple<IvMLogNotFiltered, Mode, ProcessTreeVisualisationInfo, SVGDiagram, Scaler>> input)
							throws Exception {
						IvMLog log = input.getB().getA();
						Mode colourMode = input.getB().getB();
						ProcessTreeVisualisationInfo info = input.getB().getC();
						SVGDiagram svg = input.getB().getD();
						Scaler scaler = input.getB().getE();
						org.processmining.plugins.inductiveVisualMiner.chain.ChainLink.ResettableCanceller localCanceller = input
								.getA();
						return ComputeAnimation.computeAnimation(log, colourMode, info, scaler, svg, localCanceller);
					}

				}, new InputFunction<GraphVizTokens>() {

					//this function is called on completion
					public void call(GraphVizTokens result) throws Exception {
						state.setAnimation(result);

						//update the gui (in the main thread)
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								panel.getSaveImageButton().setText("image/animation");
								panel.getGraph().setTokens(state.getAnimationGraphVizTokens());
								panel.getGraph().setAnimationExtremeTimes(
										state.getAnimationScaler().getMinInUserTime(),
										state.getAnimationScaler().getMaxInUserTime());
								panel.getGraph().setAnimationEnabled(true);
							}
						});
					}

				});
	}

	protected Double generateInput(InductiveVisualMinerState state) {

		/*
		 * The animation is independent of all other chainlinks. Therefore,
		 * compute it asynchronously.
		 */
		pool.compute(Quintuple.of(state.getIvMLog(), state.getMode(), state.getVisualisationInfo(),
				state.getSVGDiagram(), state.getAnimationScaler()), globalCanceller);

		return null;
	}

	protected Double executeLink(Double input) {
		return null;
	}

	protected void processResult(Double result, InductiveVisualMinerState state) {

	}

}

package org.processmining.plugins.inductiveVisualMiner.animation;

import java.awt.geom.NoninvertibleTransformException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nl.tue.astar.AStarThread.Canceller;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerController;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IteratorWithPosition;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.ShortestPathGraph;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLog;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMMove;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;
import org.processmining.plugins.inductiveVisualMiner.mode.Mode;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationInfo;

import com.kitfox.svg.SVGDiagram;

public class ComputeAnimation {

	public static final double initDuration = 2;
	public static final double animationDuration = 180;
	private static Random random = new Random(123);

	public static Pair<Scaler, GraphVizTokens> computeAnimation(final IvMLog ivmLog, final Mode colourMode,
			final ProcessTreeVisualisationInfo info, final SVGDiagram svg, final Canceller canceller)
			throws NoninvertibleTransformException {

		//scale timestamps
		final Scaler scaler = Scaler.fromLog(ivmLog, initDuration, animationDuration, canceller);

		if (canceller.isCancelled()) {
			return null;
		}

		//make a shortest path graph
		final ShortestPathGraph graph = new ShortestPathGraph(info.getNodes(), info.getEdges());

		if (canceller.isCancelled()) {
			return null;
		}
		
		//set up result object
		GraphVizTokens graphVizTokens = new GraphVizTokens();
		
		IteratorWithPosition<IvMTrace> it = ivmLog.iterator();
		while (it.hasNext()) {
			IvMTrace ivmTrace = it.next();
			
			//make dot tokens
			final List<DotToken> dotTokens = computeDotTokens(ivmTrace, info, colourMode, scaler, graph, canceller);
			
			//add to graphviz tokens
			DotToken2GraphVizToken.convertTokens(dotTokens, graphVizTokens, svg, it.getPosition());

			if (canceller.isCancelled()) {
				return null;
			}
		}

		if (canceller.isCancelled()) {
			return null;
		}
		
		System.out.println("animation completed " + graphVizTokens.size());

		return Pair.of(scaler, graphVizTokens);
	}

	public static List<DotToken> computeDotTokens(IvMTrace trace, final ProcessTreeVisualisationInfo info,
			final Mode colourMode, Scaler scaler, ShortestPathGraph graph, final Canceller canceller) {
		boolean showDeviations = colourMode.isShowDeviations();
		final List<DotToken> tokens = new ArrayList<>();
		try {
			//guess start and end time of the trace
			trace.setStartTime(guessStartTime(trace, graph, info, scaler));
			trace.setEndTime(guessEndTime(trace, trace.getStartTime(), graph, info, scaler));

			//compute the tokens of this trace
			tokens.add(IvMTrace2DotToken.trace2token(trace, showDeviations, graph, info, scaler));
		} catch (Exception e) {
			//for the demo, just ignore this case
			InductiveVisualMinerController.debug("trace skipped animation");
		}

		if (canceller.isCancelled()) {
			return null;
		}

		return tokens;
	}

	public static double guessStartTime(List<IvMMove> trace, ShortestPathGraph shortestGraph,
			ProcessTreeVisualisationInfo info, Scaler scaler) {
		//find the first timed move
		for (int i = 0; i < trace.size(); i++) {
			IvMMove firstTimedMove = trace.get(i);
			if (firstTimedMove.getLogTimestamp() != null) {
				//the trace ends with a fixed initialisation time
				return scaler.logTime2UserTime(firstTimedMove.getLogTimestamp() - scaler.getInitialisationInLogTime());
			}
		}

		return randomStartTime();
	}

	public static double guessEndTime(List<IvMMove> trace, double startTime, ShortestPathGraph shortestGraph,
			ProcessTreeVisualisationInfo info, Scaler scaler) {
		//find the last timed move
		for (int i = trace.size() - 1; i >= 0; i--) {
			IvMMove lastTimedMove = trace.get(i);
			if (lastTimedMove.getLogTimestamp() != null) {
				//the trace ends with a fixed initialisation time
				return scaler.logTime2UserTime(lastTimedMove.getLogTimestamp() + scaler.getInitialisationInLogTime());
			}
		}

		return randomDuration(startTime);
	}

	private static double randomStartTime() {
		double startTime;
		startTime = random.nextInt((int) (animationDuration - 10));
		return startTime;
	}

	private static double randomDuration(double startTime) {
		return startTime + random.nextInt((int) ((animationDuration - startTime) - 10)) + 10;
	}
}

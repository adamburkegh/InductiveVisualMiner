package org.processmining.plugins.inductiveVisualMiner.chain;

import org.processmining.plugins.InductiveMiner.Quadruple;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.efficienttree.UnknownTreeNodeException;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.graphviz.visualisation.DotPanelUserSettings;
import org.processmining.plugins.inductiveVisualMiner.InductiveVisualMinerState;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationData;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationDataImplEmpty;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMEfficientTree;
import org.processmining.plugins.inductiveVisualMiner.traceview.TraceViewColourMap;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisation;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationInfo;
import org.processmining.plugins.inductiveVisualMiner.visualisation.ProcessTreeVisualisationParameters;

import com.kitfox.svg.SVGDiagram;

// perform layout
public class Cl04LayoutModel
		extends
		ChainLink<Triple<IvMEfficientTree, ProcessTreeVisualisationParameters, DotPanelUserSettings>, Quadruple<Dot, SVGDiagram, ProcessTreeVisualisationInfo, TraceViewColourMap>> {

	protected Triple<IvMEfficientTree, ProcessTreeVisualisationParameters, DotPanelUserSettings> generateInput(
			InductiveVisualMinerState state) {
		return Triple.of(state.getTree(), state.getMode().getVisualisationParameters(state),
				state.getGraphUserSettings());
	}

	protected Quadruple<Dot, SVGDiagram, ProcessTreeVisualisationInfo, TraceViewColourMap> executeLink(
			Triple<IvMEfficientTree, ProcessTreeVisualisationParameters, DotPanelUserSettings> input,
			IvMCanceller canceller) throws UnknownTreeNodeException {
		//compute dot
		ProcessTreeVisualisation visualiser = new ProcessTreeVisualisation();
		AlignedLogVisualisationData data = new AlignedLogVisualisationDataImplEmpty();
		Triple<Dot, ProcessTreeVisualisationInfo, TraceViewColourMap> p = visualiser.fancy(input.getA(), data,
				input.getB());

		//set the graph direction
		input.getC().applyToDot(p.getA());

		//compute svg from dot
		SVGDiagram diagram = DotPanel.dot2svg(p.getA());

		return Quadruple.of(p.getA(), diagram, p.getB(), p.getC());
	}

	protected void processResult(Quadruple<Dot, SVGDiagram, ProcessTreeVisualisationInfo, TraceViewColourMap> result,
			InductiveVisualMinerState state) {
		state.setLayout(result.getA(), result.getB(), result.getC(), result.getD());

	}
}

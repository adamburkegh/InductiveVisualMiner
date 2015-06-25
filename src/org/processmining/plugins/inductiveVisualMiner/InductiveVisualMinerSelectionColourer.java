package org.processmining.plugins.inductiveVisualMiner;

import java.awt.Color;
import java.util.Set;

import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.graphviz.colourMaps.ColourMap;
import org.processmining.plugins.graphviz.colourMaps.ColourMaps;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.visualisation.DotPanel;
import org.processmining.plugins.inductiveVisualMiner.TraceView.TraceViewColourMap;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisationInfo;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.AlignedLogVisualisationParameters;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.LocalDotEdge;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.LocalDotNode;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignedLogInfo;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignedLogMetrics;
import org.processmining.plugins.inductiveVisualMiner.alignment.LogMovePosition;
import org.processmining.plugins.inductiveVisualMiner.animation.Animation;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.TreeUtils;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.sizeMaps.SizeMap;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;
import org.processmining.processtree.impl.AbstractTask.Manual;

import com.kitfox.svg.Group;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.Text;

public class InductiveVisualMinerSelectionColourer {

	public static void colourSelection(SVGDiagram diagram, Set<UnfoldedNode> selectedNodes,
			Set<LogMovePosition> selectedLogMoves, AlignedLogVisualisationInfo visualisationInfo) {
		for (UnfoldedNode unode : selectedNodes) {
			LocalDotNode dotNode = Animation.getDotNodeFromActivity(unode, visualisationInfo);
			colourSelectedNode(diagram, dotNode, true);
		}
		//re-colour the selected log moves
		for (LogMovePosition logMove : selectedLogMoves) {
			LocalDotEdge dotEdge = Animation.getDotEdgeFromLogMove(logMove, visualisationInfo);
			colourSelectedEdge(diagram, dotEdge, true);
		}
	}

	public static void colourSelectedNode(SVGDiagram svg, LocalDotNode dotNode, boolean selected) {
		Group svgGroup = DotPanel.getSVGElementOf(svg, dotNode);
		SVGElement shape = svgGroup.getChild(1);

		if (selected) {
			dotNode.unselectedAppearance.stroke = DotPanel.setCSSAttributeOf(shape, "stroke", "red");
			dotNode.unselectedAppearance.strokeWidth = DotPanel.setCSSAttributeOf(shape, "stroke-width", "3");
			dotNode.unselectedAppearance.strokeDashArray = DotPanel.setCSSAttributeOf(shape, "stroke-dasharray", "5,5");
		} else {
			DotPanel.setCSSAttributeOf(shape, "stroke", dotNode.unselectedAppearance.stroke);
			DotPanel.setCSSAttributeOf(shape, "stroke-width", dotNode.unselectedAppearance.strokeWidth);
			DotPanel.setCSSAttributeOf(shape, "stroke-dasharray", dotNode.unselectedAppearance.strokeDashArray);
		}
	}

	public static void colourSelectedEdge(SVGDiagram svg, LocalDotEdge dotEdge, boolean selected) {
		Group svgGroup = DotPanel.getSVGElementOf(svg, dotEdge);
		SVGElement line = svgGroup.getChild(1);
		SVGElement text = svgGroup.getChild(3);

		if (selected) {
			dotEdge.unselectedAppearance.textFill = DotPanel.setCSSAttributeOf(text, "fill", "none");
			dotEdge.unselectedAppearance.textStroke = DotPanel.setCSSAttributeOf(text, "stroke", "red");
			dotEdge.unselectedAppearance.textStrokeWidth = DotPanel.setCSSAttributeOf(text, "stroke-width", "0.55");
			dotEdge.unselectedAppearance.lineStrokeDashArray = DotPanel.setCSSAttributeOf(line, "stroke-dasharray",
					"2,5");
		} else {
			DotPanel.setCSSAttributeOf(text, "fill", dotEdge.unselectedAppearance.textFill);
			DotPanel.setCSSAttributeOf(text, "stroke", dotEdge.unselectedAppearance.textStroke);
			DotPanel.setCSSAttributeOf(text, "stroke-width", dotEdge.unselectedAppearance.textStrokeWidth);
			DotPanel.setCSSAttributeOf(line, "stroke-dasharray", dotEdge.unselectedAppearance.lineStrokeDashArray);
		}
	}

	public static TraceViewColourMap colourHighlighting(SVGDiagram svg, AlignedLogVisualisationInfo info,
			ProcessTree tree, AlignedLogInfo alignedFilteredLogInfo,
			AlignedLogVisualisationParameters visualisationParameters) {

		UnfoldedNode uroot = new UnfoldedNode(tree.getRoot());
		TraceViewColourMap colourMap = new TraceViewColourMap();

		//compute extreme cardinalities
		Pair<Long, Long> extremes = AlignedLogMetrics.getExtremes(uroot, alignedFilteredLogInfo);
		long minCardinality = extremes.getLeft();
		long maxCardinality = extremes.getRight();

		try {
			//style nodes
			for (UnfoldedNode unode : TreeUtils.unfoldAllNodes(uroot)) {
				long cardinality = AlignedLogMetrics.getNumberOfTracesRepresented(unode, alignedFilteredLogInfo);
				Pair<Color, Color> colour = styleUnfoldedNode(unode, svg, info, cardinality, minCardinality,
						maxCardinality, "", "", visualisationParameters);

				if (unode.getNode() instanceof Manual) {
					colourMap.set(unode, colour.getA(), colour.getB());
				}
			}

			//style edges
			styleEdges(svg, info, alignedFilteredLogInfo, visualisationParameters, minCardinality, maxCardinality);

		} catch (SVGException e) {
			e.printStackTrace();
		}

		return colourMap;
	}

	public static Pair<Color, Color> styleUnfoldedNode(UnfoldedNode unode, SVGDiagram svg,
			AlignedLogVisualisationInfo info, long cardinality, long minCardinality, long maxCardinality,
			String prefix, String postfix, AlignedLogVisualisationParameters visualisationParameters)
			throws SVGException {
		if (unode.getNode() instanceof Manual) {
			return styleManual(unode, svg, info, cardinality, minCardinality, maxCardinality, prefix, postfix,
					visualisationParameters);
		} else {
			styleNonManualNode(unode, svg, info, cardinality);
			return null;
		}
	}

	private static Pair<Color, Color> styleManual(UnfoldedNode unode, SVGDiagram svg, AlignedLogVisualisationInfo info,
			long cardinality, long minCardinality, long maxCardinality, String prefix, String postfix,
			AlignedLogVisualisationParameters visualisationParameters) throws SVGException {

		LocalDotNode dotNode = info.getActivityNode(unode);

		Group group = DotPanel.getSVGElementOf(svg, dotNode);
		SVGElement polygon = group.getChild(1);
		Text titleName = (Text) group.getChild(group.getChildren(null).size() - 2);
		Text titleCount = (Text) group.getChild(group.getChildren(null).size() - 1);

		//recolour the polygon
		Color fillColour;
		Color fontColour = Color.black;
		if (cardinality > 0) {
			fillColour = visualisationParameters.getColourNodes().colour(cardinality, minCardinality, maxCardinality);
			if (ColourMaps.getLuma(fillColour) < 128) {
				fontColour = Color.white;
			}
		} else {
			fillColour = visualisationParameters.getColourNodes().colour(1, 0, 2);
		}
		DotPanel.setCSSAttributeOf(polygon, "fill", fillColour);

		//set label colour
		DotPanel.setCSSAttributeOf(titleCount, "fill", fontColour);
		DotPanel.setCSSAttributeOf(titleName, "fill", fontColour);

		if (cardinality > 0) {
			DotPanel.setCSSAttributeOf(group, "opacity", "1.0");
		} else {
			DotPanel.setCSSAttributeOf(group, "opacity", "0.2");
		}

		//set title
		titleCount.getContent().clear();
		if (cardinality >= 0) {
			titleCount.getContent().add(prefix + cardinality + postfix);
		} else {
			titleCount.getContent().add("-");
		}
		titleCount.rebuild();

		return Pair.of(fillColour, fontColour);
	}

	private static void styleNonManualNode(UnfoldedNode unode, SVGDiagram svg, AlignedLogVisualisationInfo info,
			long cardinality) {
		//colour non-activity nodes
		for (LocalDotNode dotNode : info.getNodes(unode)) {
			if (cardinality > 0) {
				DotPanel.setCSSAttributeOf(svg, dotNode, "opacity", "1.0");
			} else {
				DotPanel.setCSSAttributeOf(svg, dotNode, "opacity", "0.2");
			}
		}
	}

	private static void styleEdges(SVGDiagram svg, AlignedLogVisualisationInfo info, AlignedLogInfo logInfo,
			AlignedLogVisualisationParameters parameters, long minCardinality, long maxCardinality) throws SVGException {
		styleModelEdges(svg, info, logInfo, parameters, minCardinality, maxCardinality);
		styleMoveEdges(svg, info, logInfo, parameters, minCardinality, maxCardinality);
	}

	private static void styleModelEdges(SVGDiagram svg, AlignedLogVisualisationInfo info, AlignedLogInfo logInfo,
			AlignedLogVisualisationParameters parameters, long minCardinality, long maxCardinality) throws SVGException {
		for (LocalDotEdge dotEdge : info.getAllModelEdges()) {
			long cardinality;
			cardinality = AlignedLogMetrics.getNumberOfTracesRepresented(dotEdge.getUnode(), logInfo);
			styleEdge(dotEdge, svg, cardinality, minCardinality, maxCardinality, parameters.getColourModelEdges(),
					parameters.isShowFrequenciesOnModelEdges(), parameters.getModelEdgesWidth());
		}
	}

	private static void styleMoveEdges(SVGDiagram svg, AlignedLogVisualisationInfo info, AlignedLogInfo logInfo,
			AlignedLogVisualisationParameters parameters, long minCardinality, long maxCardinality) throws SVGException {
		//style model move edges
		for (LocalDotEdge dotEdge : info.getAllModelMoveEdges()) {
			long cardinality = AlignedLogMetrics.getModelMovesLocal(dotEdge.getUnode(), logInfo);
			styleEdge(dotEdge, svg, cardinality, minCardinality, maxCardinality, parameters.getColourMoves(),
					parameters.isShowFrequenciesOnMoveEdges(), parameters.getMoveEdgesWidth());
		}

		//style log moves
		for (LocalDotEdge dotEdge : info.getAllLogMoveEdges()) {
			LogMovePosition logMovePosition = LogMovePosition.of(dotEdge);
			long cardinality = AlignedLogMetrics.getLogMoves(logMovePosition, logInfo).size();
			styleEdge(dotEdge, svg, cardinality, minCardinality, maxCardinality, parameters.getColourMoves(),
					parameters.isShowFrequenciesOnMoveEdges(), parameters.getMoveEdgesWidth());
		}
	}

	private static void styleEdge(DotEdge edge, SVGDiagram svg, long cardinality, long minCardinality,
			long maxCardinality, ColourMap colourMap, boolean showFrequency, SizeMap widthMap) throws SVGException {

		//prepare parts of the rendered dot element
		Group group = DotPanel.getSVGElementOf(svg, edge);
		SVGElement line = group.getChild(1);
		SVGElement arrowHead = group.getChild(2);

		//stroke
		Color edgeColour = colourMap.colour(cardinality, minCardinality, maxCardinality);
		double strokeWidth = widthMap.size(cardinality, minCardinality, maxCardinality);
		DotPanel.setCSSAttributeOf(line, "stroke", edgeColour);
		DotPanel.setCSSAttributeOf(arrowHead, "stroke", edgeColour);
		DotPanel.setCSSAttributeOf(arrowHead, "fill", edgeColour);
		DotPanel.setCSSAttributeOf(line, "stroke-width", strokeWidth + "");

		//transparency
		if (cardinality > 0) {
			DotPanel.setCSSAttributeOf(group, "opacity", "1.0");
		} else {
			DotPanel.setCSSAttributeOf(group, "opacity", "0.1");
		}

		//edge label
		if (showFrequency) {
			Text label = (Text) group.getChild(group.getChildren(null).size() - 1);
			label.getContent().clear();
			label.getContent().add(cardinality + "");
			label.rebuild();
		}
	}
}

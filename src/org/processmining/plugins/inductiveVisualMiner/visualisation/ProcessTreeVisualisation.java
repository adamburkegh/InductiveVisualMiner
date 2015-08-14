package org.processmining.plugins.inductiveVisualMiner.visualisation;

import java.awt.Color;
import java.util.Iterator;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.mining.interleaved.Interleaved;
import org.processmining.plugins.graphviz.colourMaps.ColourMap;
import org.processmining.plugins.graphviz.colourMaps.ColourMaps;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.inductiveVisualMiner.TraceView.TraceViewColourMap;
import org.processmining.plugins.inductiveVisualMiner.alignedLogVisualisation.data.AlignedLogVisualisationData;
import org.processmining.plugins.inductiveVisualMiner.alignment.LogMovePosition;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotEdge.EdgeType;
import org.processmining.plugins.inductiveVisualMiner.visualisation.LocalDotNode.NodeType;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.UnfoldedNode;
import org.processmining.processtree.impl.AbstractBlock.And;
import org.processmining.processtree.impl.AbstractBlock.Or;
import org.processmining.processtree.impl.AbstractBlock.Seq;
import org.processmining.processtree.impl.AbstractBlock.Xor;
import org.processmining.processtree.impl.AbstractBlock.XorLoop;
import org.processmining.processtree.impl.AbstractTask.Automatic;
import org.processmining.processtree.impl.AbstractTask.Manual;

public class ProcessTreeVisualisation {

	private long maxCardinality;
	private long minCardinality;
	ProcessTreeVisualisationParameters parameters;

	private AlignedLogVisualisationData data;

	private Dot dot;
	private ProcessTreeVisualisationInfo info;
	private TraceViewColourMap traceViewColourMap;

	public Triple<Dot, ProcessTreeVisualisationInfo, TraceViewColourMap> fancy(ProcessTree tree,
			AlignedLogVisualisationData data, ProcessTreeVisualisationParameters parameters) {
		this.parameters = parameters;
		this.data = data;

		//find maximum and mimimum occurrences
		Pair<Long, Long> p = data.getExtremeCardinalities();
		minCardinality = p.getLeft();
		maxCardinality = p.getRight();

		dot = new Dot();
		dot.setDirection(GraphDirection.leftRight);
		UnfoldedNode root = new UnfoldedNode(tree.getRoot());

		traceViewColourMap = new TraceViewColourMap();

		//source & sink
		info = new ProcessTreeVisualisationInfo();
		LocalDotNode source = new LocalDotNode(dot, info, NodeType.source, "", root);
		LocalDotNode sink = new LocalDotNode(dot, info, NodeType.sink, "", root);
		info.setRoot(source, sink);
		//convert root node
		convertNode(root, source, sink, true);

		//add log-move-arcs to source and sink
		//a parallel root will project its own log moves 
		if (parameters.isShowLogMoves() && !(root.getBlock() instanceof And)) {
			visualiseLogMove(source, source, root, LogMovePosition.atSource(root), true);
			visualiseLogMove(sink, sink, root, LogMovePosition.atSink(root), false);
		}

		return Triple.of(dot, info, traceViewColourMap);
	}

	private void convertNode(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {
		if (unode.getNode() instanceof Seq) {
			convertSequence(unode, source, sink, directionForward);
		} else if (unode.getNode() instanceof XorLoop) {
			convertLoop(unode, source, sink, directionForward);
		} else if (unode.getNode() instanceof Interleaved) {
			convertInterleaved(unode, source, sink, directionForward);
		} else if (unode.getNode() instanceof And) {
			convertParallel(unode, source, sink, directionForward);
		} else if (unode.getNode() instanceof Or) {
			convertOr(unode, source, sink, directionForward);
		} else if (unode.getNode() instanceof Xor) {
			convertXor(unode, source, sink, directionForward);
		} else if (unode.getNode() instanceof Manual) {
			convertActivity(unode, source, sink, directionForward);
		} else if (unode.getNode() instanceof Automatic) {
			convertTau(unode, source, sink, directionForward);
		}
	}

	private void convertActivity(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {
		Pair<String, Long> cardinality = data.getNodeLabel(unode, false);
		LocalDotNode dotNode = convertActivity(unode, cardinality);

		addArc(source, dotNode, unode, directionForward, false);
		addArc(dotNode, sink, unode, directionForward, false);

		//draw model moves
		if (parameters.isShowModelMoves()) {
			Pair<String, Long> modelMoves = data.getModelMoveEdgeLabel(unode);
			if (modelMoves.getB() != 0) {
				addMoveArc(source, sink, unode, EdgeType.modelMove, null, null, modelMoves, directionForward);
			}
		}

		//draw log moves
		if (parameters.isShowLogMoves()) {
			visualiseLogMove(dotNode, dotNode, unode, LogMovePosition.onLeaf(unode), directionForward);
		}
	}

	private LocalDotNode convertActivity(UnfoldedNode unode, Pair<String, Long> cardinality) {
		//style the activity by the occurrences of it
		Color fillColour = Color.white;
		if (cardinality.getB() != 0 && parameters.getColourNodes() != null) {
			fillColour = parameters.getColourNodes().colour((long) (getOccurrenceFactor(cardinality.getB()) * 100), 0,
					100);
		}

		//determine label colour
		Color fontColour = Color.black;
		if (ColourMaps.getLuma(fillColour) < 128) {
			fontColour = Color.white;
		}
		traceViewColourMap.set(unode, fillColour, fontColour);

		String label = unode.getNode().getName();
		if (label.length() == 0) {
			label = " ";
		}
		if (!cardinality.getA().isEmpty()) {
			label += "\n" + cardinality.getA();
		}

		final LocalDotNode dotNode = new LocalDotNode(dot, info, NodeType.activity, label, unode);
		dotNode.setOption("fillcolor", ColourMap.toHexString(fillColour));
		dotNode.setOption("fontcolor", ColourMap.toHexString(fontColour));

		info.addNode(unode, dotNode);
		return dotNode;
	}

	private void convertTau(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {
		addArc(source, sink, unode, directionForward, false);
	}

	private void convertSequence(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {
		LocalDotNode split;
		LocalDotNode join = source;

		Iterator<Node> it = unode.getBlock().getChildren().iterator();
		while (it.hasNext()) {
			Node child = it.next();

			split = join;
			if (it.hasNext()) {
				join = new LocalDotNode(dot, info, NodeType.xor, "", unode);
			} else {
				join = sink;
			}

			convertNode(unode.unfoldChild(child), split, join, directionForward);

			//draw log-move-arc if necessary
			if (parameters.isShowLogMoves()) {
				visualiseLogMove(split, split, unode, LogMovePosition.beforeChild(unode, unode.unfoldChild(child)),
						directionForward);
			}
		}
	}

	private void convertLoop(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.xor, "", unode);
		addArc(source, split, unode, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.xor, "", unode);

		Node bodyChild = unode.getBlock().getChildren().get(0);
		convertNode(unode.unfoldChild(bodyChild), split, join, directionForward);

		Node redoChild = unode.getBlock().getChildren().get(1);
		convertNode(unode.unfoldChild(redoChild), join, split, !directionForward);

		Node exitChild = unode.getBlock().getChildren().get(2);
		convertNode(unode.unfoldChild(exitChild), join, sink, directionForward);

		//put log-moves on children
		if (parameters.isShowLogMoves()) {
			visualiseLogMove(split, split, unode, LogMovePosition.beforeChild(unode, unode.unfoldChild(bodyChild)),
					directionForward);
			visualiseLogMove(join, join, unode, LogMovePosition.beforeChild(unode, unode.unfoldChild(redoChild)),
					directionForward);

			//log moves can be projected before the exit-tau
			//(assume it's tau)
			info.registerExtraEdge(unode, unode.unfoldChild(exitChild),
					info.getLogMoveEdge(unode, unode.unfoldChild(redoChild)));
		}
	}

	private void convertParallel(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.parallelSplit, "+", unode);
		addArc(source, split, unode, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.parallelJoin, "+", unode);
		addArc(join, sink, unode, directionForward, true);

		for (Node child : unode.getBlock().getChildren()) {
			convertNode(unode.unfoldChild(child), split, join, directionForward);
		}

		//put log-moves, if necessary
		if (parameters.isShowLogMoves()) {
			//on split
			visualiseLogMove(split, split, unode, LogMovePosition.atSource(unode), directionForward);

			//on join
			visualiseLogMove(join, join, unode, LogMovePosition.atSink(unode), directionForward);
		}
	}
	
	private void convertInterleaved(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.interleavedSplit, "\u2194", unode);
		addArc(source, split, unode, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.interleavedJoin, "\u2194", unode);
		addArc(join, sink, unode, directionForward, true);

		for (Node child : unode.getBlock().getChildren()) {
			convertNode(unode.unfoldChild(child), split, join, directionForward);
		}

		//put log-moves, if necessary
		if (parameters.isShowLogMoves()) {
			//on split
			visualiseLogMove(split, split, unode, LogMovePosition.atSource(unode), directionForward);

			//on join
			visualiseLogMove(join, join, unode, LogMovePosition.atSink(unode), directionForward);
		}
	}

	private void convertOr(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.parallelSplit, "o", unode);
		addArc(source, split, unode, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.parallelJoin, "o", unode);
		addArc(join, sink, unode, directionForward, true);

		for (Node child : unode.getBlock().getChildren()) {
			convertNode(unode.unfoldChild(child), split, join, directionForward);
		}

		//put log-moves, if necessary
		if (parameters.isShowLogMoves()) {
			//on split
			visualiseLogMove(split, split, unode, LogMovePosition.atSource(unode), directionForward);

			//on join
			visualiseLogMove(join, join, unode, LogMovePosition.atSink(unode), directionForward);
		}
	}

	private void convertXor(UnfoldedNode unode, LocalDotNode source, LocalDotNode sink, boolean directionForward) {

		//operator split
		LocalDotNode split = new LocalDotNode(dot, info, NodeType.xor, "", unode);
		addArc(source, split, unode, directionForward, true);

		//operator join
		LocalDotNode join = new LocalDotNode(dot, info, NodeType.xor, "", unode);
		addArc(join, sink, unode, directionForward, true);

		for (Node child : unode.getBlock().getChildren()) {
			convertNode(unode.unfoldChild(child), split, join, directionForward);
		}

		//log-moves
		//are never put on xor
	}

	private LocalDotEdge addArc(final LocalDotNode from, final LocalDotNode to, final UnfoldedNode unode,
			boolean directionForward, boolean includeModelMoves) {
		return addModelArc(from, to, unode, directionForward, data.getEdgeLabel(unode, includeModelMoves));
	}

	private LocalDotEdge addModelArc(final LocalDotNode from, final LocalDotNode to, final UnfoldedNode unode,
			final boolean directionForward, final Pair<String, Long> cardinality) {

		final LocalDotEdge edge;
		if (directionForward) {
			edge = new LocalDotEdge(dot, info, from, to, "", unode, EdgeType.model, null, null, directionForward);
		} else {
			edge = new LocalDotEdge(dot, info, to, from, "", unode, EdgeType.model, null, null, directionForward);
			edge.setOption("dir", "back");
		}

		if (parameters.getColourModelEdges() != null) {
			String lineColour = parameters.getColourModelEdges().colourString(cardinality.getB(), minCardinality,
					maxCardinality);
			edge.setOption("color", lineColour);
		}

		edge.setOption("penwidth",
				"" + parameters.getModelEdgesWidth().size(cardinality.getB(), minCardinality, maxCardinality));

		if (parameters.isShowFrequenciesOnModelEdges() && !cardinality.getA().isEmpty()) {
			edge.setLabel(cardinality.getA());
		} else {
			edge.setLabel(" ");
		}

		return edge;
	}

	private void visualiseLogMove(LocalDotNode from, LocalDotNode to, UnfoldedNode unode,
			LogMovePosition logMovePosition, boolean directionForward) {
		Pair<String, MultiSet<XEventClass>> logMoves = data.getLogMoveEdgeLabel(logMovePosition);
		Pair<String, Long> t = Pair.of(logMoves.getA(), logMoves.getB().size());
		if (logMoves.getB().size() > 0) {
			if (parameters.isRepairLogMoves()) {
				for (XEventClass e : logMoves.getB()) {
					long cardinality = logMoves.getB().getCardinalityOf(e);
					LocalDotNode dotNode = new LocalDotNode(dot, info, NodeType.logMoveActivity, e.toString(), unode);
					addMoveArc(from, dotNode, unode, EdgeType.logMove, logMovePosition.getOn(),
							logMovePosition.getBeforeChild(), t, directionForward);
					addMoveArc(dotNode, to, unode, EdgeType.logMove, logMovePosition.getOn(),
							logMovePosition.getBeforeChild(), t, directionForward);
				}
			} else {
				addMoveArc(from, to, unode, EdgeType.logMove, logMovePosition.getOn(),
						logMovePosition.getBeforeChild(), t, directionForward);
			}
		}
	}

	private LocalDotEdge addMoveArc(LocalDotNode from, LocalDotNode to, UnfoldedNode unode, EdgeType type,
			UnfoldedNode lookupNode1, UnfoldedNode lookupNode2, Pair<String, Long> cardinality, boolean directionForward) {

		LocalDotEdge edge;
		if (directionForward) {
			edge = new LocalDotEdge(dot, info, from, to, "", unode, type, lookupNode1, lookupNode2, directionForward);
		} else {
			edge = new LocalDotEdge(dot, info, to, from, "", unode, type, lookupNode1, lookupNode2, directionForward);
			edge.setOption("dir", "back");
		}

		edge.setOption("style", "dashed");
		edge.setOption("arrowsize", ".5");

		if (parameters.getColourMoves() != null) {
			String lineColour = parameters.getColourMoves().colourString(cardinality.getB(), minCardinality,
					maxCardinality);
			edge.setOption("color", lineColour);
			edge.setOption("fontcolor", lineColour);
		}

		edge.setOption("penwidth",
				"" + parameters.getMoveEdgesWidth().size(cardinality.getB(), minCardinality, maxCardinality));

		if (parameters.isShowFrequenciesOnMoveEdges()) {
			edge.setLabel(cardinality.getA());
		} else {
			edge.setLabel(" ");
		}

		return edge;
	}

	private double getOccurrenceFactor(long cardinality) {
		return ProcessTreeVisualisationHelper.getOccurrenceFactor(cardinality, minCardinality, maxCardinality);
	}
}

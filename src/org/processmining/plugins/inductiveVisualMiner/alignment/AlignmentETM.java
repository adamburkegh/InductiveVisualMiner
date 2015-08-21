package org.processmining.plugins.inductiveVisualMiner.alignment;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nl.tue.astar.AStarThread.Canceller;
import nl.tue.astar.Trace;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.MultiSet;
import org.processmining.plugins.InductiveMiner.Triple;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.model.narytree.NAryTree;
import org.processmining.plugins.etm.model.narytree.conversion.ProcessTreeToNAryTree;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.inductiveVisualMiner.alignment.Move.Type;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.TreeUtils;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.Alignment2IvMLog;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogBase;
import org.processmining.plugins.inductiveVisualMiner.performance.ExpandProcessTreeForQueues;
import org.processmining.plugins.inductiveVisualMiner.performance.Performance;
import org.processmining.plugins.inductiveVisualMiner.performance.Performance.PerformanceTransition;
import org.processmining.plugins.inductiveVisualMiner.performance.XEventPerformanceClassifier;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractTask.Automatic;
import org.processmining.processtree.impl.AbstractTask.Manual;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.UnfoldedNode;

public class AlignmentETM {

	public static IvMLogBase align(ProcessTree tree, XEventPerformanceClassifier performanceClassifier,
			XLog xLog, IMLog log, XEventClasses activityEventClasses, XEventClasses performanceEventClasses, Canceller canceller) {

		CentralRegistry registry = new CentralRegistry(xLog, performanceClassifier, new Random());

		//transform tree for performance measurement
		Triple<ProcessTree, Map<UnfoldedNode, UnfoldedNode>, Set<UnfoldedNode>> t = ExpandProcessTreeForQueues
				.expand(tree);
		ProcessTree performanceTree = t.getA();
		Map<UnfoldedNode, UnfoldedNode> performanceNodeMapping = t.getB(); //mapping performance node -> original node
		Set<UnfoldedNode> enqueueTaus = t.getC(); //set of taus involved in enqueueing

		//add the event classes of the tree manually
		addAllLeaves(registry.getEventClasses(), performanceTree.getRoot());

		ProcessTreeToNAryTree pt2nt = new ProcessTreeToNAryTree(registry.getEventClasses());
		NAryTree nTree = pt2nt.convert(performanceTree);

		//tell ETM that event classes were added
		registry.updateLogDerived();

		//perform the alignment
		FitnessReplay fr = new FitnessReplay(registry, canceller);
		fr.setNrThreads(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		fr.setDetailedAlignmentInfoEnabled(true);
		fr.getFitness(nTree, null);
		BehaviorCounter behC = registry.getFitness(nTree).behaviorCounter;

		//create mapping int->(performance)unfoldedNode
		List<UnfoldedNode> l = TreeUtils.unfoldAllNodes(new UnfoldedNode(performanceTree.getRoot()));
		UnfoldedNode[] nodeId2performanceNode = l.toArray(new UnfoldedNode[l.size()]);

		//read moves and create aligned log
		MultiSet<AlignedTrace> alignedLog = new MultiSet<>();
		for (Trace naryTrace : behC.getAlignments().keySet()) {

			AlignedTrace trace = new AlignedTrace();

			TreeRecord tr = behC.getAlignment(naryTrace);
			List<TreeRecord> naryMoves = TreeRecord.getHistory(tr);
			long cardinality = registry.getaStarAlgorithm().getTraceFreq(naryTrace);

			for (TreeRecord naryMove : naryMoves) {

				//get log part of move
				XEventClass performanceActivity = null;
				XEventClass activity = null;
				PerformanceTransition lifeCycleTransition = null;
				if (naryMove.getMovedEvent() >= 0) {
					//an ETM-log-move happened
					performanceActivity = registry.getEventClassByID(naryTrace.get(naryMove.getMovedEvent()));
					activity = Performance.getActivity(performanceActivity, activityEventClasses);
					lifeCycleTransition = Performance.getLifeCycleTransition(performanceActivity);
				}

				//get model part of move
				UnfoldedNode performanceUnode = null;
				UnfoldedNode unode = null;
				if (naryMove.getModelMove() >= 0) {
					//an ETM-model-move happened
					performanceUnode = nodeId2performanceNode[naryMove.getModelMove()];
					unode = performanceNodeMapping.get(performanceUnode);
					lifeCycleTransition = Performance.getLifeCycleTransition(performanceUnode);

					if (performanceUnode.getNode() instanceof Automatic && unode.getNode() instanceof Manual) {
						//this is a tau that represents that the start/enqueue of an activity is skipped
						if (enqueueTaus.contains(performanceUnode)) {
							lifeCycleTransition = PerformanceTransition.enqueue;
							performanceActivity = performanceEventClasses.getByIdentity(unode.getNode().getName()
									+ "+enqueue");
						} else {
							lifeCycleTransition = PerformanceTransition.start;
							performanceActivity = performanceEventClasses.getByIdentity(unode.getNode().getName() + "+"
									+ XLifecycleExtension.StandardModel.START);
						}
						activity = activityEventClasses.getByIdentity(unode.getNode().getName());
					} else if (performanceUnode.getNode() instanceof Automatic) {
						//a model move happened on a tau; make it a completion
						lifeCycleTransition = PerformanceTransition.complete;
					}

					//we are only interested in moves on leaves, not in moves on nodes
					if (!(performanceUnode.getNode() instanceof Manual)
							&& !(performanceUnode.getNode() instanceof Automatic)) {
						continue;
					}
				}

				if (performanceUnode != null || performanceActivity != null) {
					Move move;
					if (performanceUnode != null && performanceUnode.getNode() instanceof Automatic
							&& unode.getNode() instanceof Manual) {
						//tau-start
						move = new Move(Type.ignoredModelMove, unode, activity, performanceActivity,
								lifeCycleTransition);
					} else if ((performanceUnode != null && performanceActivity != null)
							|| (performanceUnode != null && performanceUnode.getNode() instanceof Automatic)) {
						//synchronous move
						move = new Move(Type.synchronousMove, unode, activity, performanceActivity, lifeCycleTransition);
					} else if (performanceUnode != null) {
						//model move
						move = new Move(Type.modelMove, unode, activity, performanceActivity, lifeCycleTransition);
					} else {
						//log move
						if (lifeCycleTransition == PerformanceTransition.complete) {
							//only log moves of complete events are interesting
							move = new Move(Type.logMove, unode, activity, performanceActivity, lifeCycleTransition);
						} else {
							//log moves of other transitions are ignored
							move = new Move(Type.ignoredLogMove, null, activity, performanceActivity,
									lifeCycleTransition);
						}
					}
					trace.add(move);
				}
			}
			alignedLog.add(trace, cardinality);
		}

		return Alignment2IvMLog.convert(alignedLog, log, performanceEventClasses, canceller);
	}

	public static void addAllLeaves(XEventClasses classes, Node node) {
		if (node instanceof Manual) {
			classes.register(node.getName());
		} else if (node instanceof Block) {
			for (Node child : ((Block) node).getChildren()) {
				addAllLeaves(classes, child);
			}
		}
		classes.harmonizeIndices();
	}

}

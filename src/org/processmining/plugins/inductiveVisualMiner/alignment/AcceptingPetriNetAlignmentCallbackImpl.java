package org.processmining.plugins.inductiveVisualMiner.alignment;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.InductiveMiner.Sextuple;
import org.processmining.plugins.inductiveVisualMiner.alignment.Move.Type;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFilteredImpl;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTraceImpl;
import org.processmining.plugins.inductiveVisualMiner.performance.Performance;
import org.processmining.plugins.inductiveVisualMiner.performance.Performance.PerformanceTransition;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;

import gnu.trove.map.TObjectIntMap;

public class AcceptingPetriNetAlignmentCallbackImpl implements AcceptingPetriNetAlignmentCallback {

	//input
	private final XLog xLog;
	private final IvMModel model;
	private final IvMEventClasses activityEventClasses;

	private final TObjectIntMap<Transition> activity2skipEnqueue;
	private final TObjectIntMap<Transition> activity2skipStart;
	private final Set<Transition> startTransitions;
	private final Set<Transition> endTransitions;
	private final Set<Transition> interTransitions;

	//output
	private final IvMLogNotFilteredImpl alignedLog;

	public AcceptingPetriNetAlignmentCallbackImpl(XLog xLog, IvMModel model, IvMEventClasses activityEventClasses,
			Sextuple<AcceptingPetriNet, TObjectIntMap<Transition>, TObjectIntMap<Transition>, Set<Transition>, Set<Transition>, Set<Transition>> p) {
		this.xLog = xLog;
		this.model = model;
		this.activityEventClasses = activityEventClasses;

		this.activity2skipEnqueue = p.getB();
		this.activity2skipStart = p.getC();
		this.startTransitions = p.getD();
		this.endTransitions = p.getE();
		this.interTransitions = p.getF();

		alignedLog = new IvMLogNotFilteredImpl(xLog.size());
	}

	public void traceAlignmentComplete(SyncReplayResult aTrace, SortedSet<Integer> xTraces,
			IvMEventClasses performanceEventClasses) {

		for (Integer traceIndex : xTraces) {
			XTrace xTrace = xLog.get(traceIndex);

			//get trace name
			String traceName;
			if (xTrace.getAttributes().containsKey("concept:name")) {
				traceName = xTrace.getAttributes().get("concept:name").toString();
			} else {
				traceName = "";
			}

			IvMTrace iTrace = new IvMTraceImpl(traceName, xTrace.getAttributes(), aTrace.getNodeInstance().size());
			Iterator<StepTypes> itType = aTrace.getStepTypes().iterator();
			Iterator<Object> itNode = aTrace.getNodeInstance().iterator();
			int eventIndex = 0;
			while (itType.hasNext()) {
				StepTypes type = itType.next();
				Object node = itNode.next();
				Move move = getMove(xTrace, type, node, performanceEventClasses, eventIndex);

				if (move != null) {
					iTrace.add(ETMAlignmentCallbackImpl.move2ivmMove(model, move, xTrace, eventIndex));
				}

				if (move != null && (type == StepTypes.L || type == StepTypes.LMGOOD)) {
					eventIndex++;
				}
			}

			alignedLog.set(traceIndex, iTrace);
		}
	}

	private Move getMove(XTrace trace, StepTypes type, Object node, IvMEventClasses performanceEventClasses,
			int event) {

		//get log part of move
		if (type == StepTypes.L) {
			//a log-move happened
			XEventClass performanceActivity = (XEventClass) node;
			XEventClass activity = Performance.getActivity(performanceActivity, activityEventClasses);
			PerformanceTransition lifeCycleTransition = Performance.getLifeCycleTransition(performanceActivity);

			//log move
			if (lifeCycleTransition == PerformanceTransition.complete) {
				//only log moves of complete events are interesting
				return new Move(model, Type.logMove, -1, activity, performanceActivity, lifeCycleTransition);
			} else {
				//log moves of other transitions are ignored
				return new Move(model, Type.ignoredLogMove, -1, activity, performanceActivity, lifeCycleTransition);
			}
		} else if (type == StepTypes.MINVI) {
			//enqueue- or start-skip, or start-tau
			if (startTransitions.contains(node) || endTransitions.contains(node) || interTransitions.contains(node)) {
				//start-tau
				return null;
			} else {
				//enqueue- or start-skip
				assert (node instanceof Transition);
				Transition performanceUnode = (Transition) node;

				PerformanceTransition lifeCycleTransition;
				int activityIndex;
				if (activity2skipEnqueue.containsKey(performanceUnode)) {
					lifeCycleTransition = PerformanceTransition.enqueue;
					activityIndex = activity2skipEnqueue.get(performanceUnode);
				} else {
					lifeCycleTransition = PerformanceTransition.start;
					activityIndex = activity2skipStart.get(performanceUnode);
				}
				XEventClass activity = model.getDfg().getActivityOfIndex(activityIndex);
				XEventClass performanceActivity = performanceEventClasses
						.getByIdentity(activity.getId() + "+" + lifeCycleTransition);
				return new Move(model, Type.ignoredModelMove, model.getDfg().getIndexOfActivity(activity), activity,
						performanceActivity, lifeCycleTransition);
			}
		} else if (type == StepTypes.MREAL) {
			//model move
			assert (node instanceof Transition);
			Transition performanceUnode = (Transition) node;
			PerformanceTransition lifeCycleTransition = Performance.getLifeCycleTransition(performanceUnode.getLabel());
			XEventClass performanceActivity = performanceEventClasses.getByIdentity(((Transition) node).getLabel());
			XEventClass activity = Performance.getActivity(performanceActivity, activityEventClasses);
			return new Move(model, Type.modelMove, model.getDfg().getIndexOfActivity(activity), activity,
					performanceActivity, lifeCycleTransition);
		} else if (type == StepTypes.LMGOOD) {
			assert (node instanceof Transition);
			Transition performanceUnode = (Transition) node;
			XEventClass performanceActivity = performanceEventClasses.getClassOf(trace.get(event));
			XEventClass activity = Performance.getActivity(performanceActivity, activityEventClasses);
			PerformanceTransition lifeCycleTransition = Performance.getLifeCycleTransition(performanceUnode.getLabel());
			return new Move(model, Type.synchronousMove, model.getDfg().getIndexOfActivity(activity), activity,
					performanceActivity, lifeCycleTransition);
		}
		return null;
	}

	public void alignmentFailed() throws Exception {
		// TODO Auto-generated method stub

	}

	public IvMLogNotFiltered getAlignedLog() {
		return alignedLog;
	}

}

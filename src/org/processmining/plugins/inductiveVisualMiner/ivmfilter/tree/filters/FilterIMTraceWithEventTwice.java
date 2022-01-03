package org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.filters;

import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.InductiveMiner.mining.logs.IMTrace;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecoratorI;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.IvMFilterGui;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterBuilderAbstract;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNode;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNodeCompositeAbstract;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;

public class FilterIMTraceWithEventTwice extends IvMFilterBuilderAbstract<IMTrace, XEvent, IvMFilterGui> {

	@Override
	public String toString() {
		return "with event twice";
	}

	@Override
	public String toString(IvMFilterGui panel) {
		return "with event twice";
	}

	@Override
	public IvMFilterGui createGui(Runnable onUpdate, IvMDecoratorI decorator) {
		IvMFilterGui result = new IvMFilterGui(toString(), decorator);
		result.add(result.createExplanation("Include traces that have two events that pass all the sub-filters."));
		return result;
	}

	@Override
	public IvMFilterTreeNode<IMTrace> buildFilter(IvMFilterGui gui) {
		return new IvMFilterTreeNodeCompositeAbstract<IMTrace, XEvent>() {

			private static final long serialVersionUID = 8213030059677606305L;

			public boolean staysInLogA(IMTrace x) {
				boolean first = false;
				for (XEvent move : x) {
					if (targets(move)) {
						if (first) {
							return true;
						} else {
							first = true;
						}
					}
				}
				return false;
			}

			private boolean targets(XEvent move) {
				for (IvMFilterTreeNode<XEvent> child : this) {
					if (!child.staysInLog(move)) {
						return false;
					}
				}
				return true;
			}

			@Override
			public String getPrefix() {
				return "two events ";
			}

			public String getDivider() {
				return "and";
			}

			public boolean couldSomethingBeFiltered() {
				return true; //the empty trace and traces with a single event are always filtered
			}
		};
	}

	@Override
	public boolean allowsChildren() {
		return true;
	}

	@Override
	public Class<IMTrace> getTargetClass() {
		return IMTrace.class;
	}

	@Override
	public Class<XEvent> getChildrenTargetClass() {
		return XEvent.class;
	}

	@Override
	public void setAttributesInfo(AttributesInfo attributesInfo, IvMFilterGui gui) {

	}
}
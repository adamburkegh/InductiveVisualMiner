package org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.filters;

import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecoratorI;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.IvMFilterGui;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterBuilderAbstract;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNode;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNodeCompositeAbstract;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;

public class FilterXEventAnd extends IvMFilterBuilderAbstract<XEvent, XEvent, IvMFilterGui> {

	@Override
	public String toString() {
		return "and";
	}

	@Override
	public String toString(IvMFilterGui panel) {
		return "and";
	}

	@Override
	public IvMFilterGui createGui(Runnable onUpdate, IvMDecoratorI decorator) {
		IvMFilterGui result = new IvMFilterGui(null, decorator);
		result.add(result.createExplanation("Include events that pass all of the sub-filters."));
		return result;
	}

	@Override
	public IvMFilterTreeNode<XEvent> buildFilter(IvMFilterGui panel) {
		return new IvMFilterTreeNodeCompositeAbstract<XEvent, XEvent>() {

			private static final long serialVersionUID = -2705606899973613204L;

			public boolean staysInLogA(XEvent x) {
				for (IvMFilterTreeNode<XEvent> child : this) {
					if (!child.staysInLog(x)) {
						return false;
					}
				}
				return true;
			}

			@Override
			public String getPrefix() {
				return "both";
			}

			public String getDivider() {
				return "and";
			}

			public boolean couldSomethingBeFiltered() {
				for (IvMFilterTreeNode<XEvent> child : this) {
					if (child.couldSomethingBeFiltered()) {
						return true;
					}
				}
				return false;
			}
		};
	}

	@Override
	public boolean allowsChildren() {
		return true;
	}

	@Override
	public Class<XEvent> getTargetClass() {
		return XEvent.class;
	}

	@Override
	public Class<XEvent> getChildrenTargetClass() {
		return XEvent.class;
	}

	@Override
	public void setAttributesInfo(AttributesInfo attributesInfo, IvMFilterGui gui) {

	}

}

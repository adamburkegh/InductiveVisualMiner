package org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.filters;

import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecoratorI;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.IvMFilterGui;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterBuilderAbstract;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNode;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNodeCompositeAbstract;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;
import org.processmining.plugins.inductiveminer2.attributes.AttributesInfo;

public class FilterIvMTraceAnd extends IvMFilterBuilderAbstract<IvMTrace, IvMTrace, IvMFilterGui> {

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
		result.add(result.createExplanation("Include traces that pass all of the sub-filters."));
		return result;
	}

	@Override
	public IvMFilterTreeNode<IvMTrace> buildFilter(IvMFilterGui panel) {
		return new IvMFilterTreeNodeCompositeAbstract<IvMTrace, IvMTrace>() {

			private static final long serialVersionUID = -2705606899973613204L;

			public boolean staysInLogA(IvMTrace x) {
				for (IvMFilterTreeNode<IvMTrace> child : this) {
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
				for (IvMFilterTreeNode<IvMTrace> child : this) {
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
	public Class<IvMTrace> getTargetClass() {
		return IvMTrace.class;
	}

	@Override
	public Class<IvMTrace> getChildrenTargetClass() {
		return IvMTrace.class;
	}

	@Override
	public void setAttributesInfo(AttributesInfo attributesInfo, IvMFilterGui gui) {

	}

}

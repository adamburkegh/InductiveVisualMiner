package org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.filters;

import java.awt.Color;

import org.processmining.plugins.inductiveVisualMiner.attributes.IvMAttributesInfo;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecoratorI;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.IvMFilterGui;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterBuilder;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNode;
import org.processmining.plugins.inductiveVisualMiner.ivmfilter.tree.IvMFilterTreeNodeCompositeAbstract;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;

public class FilterIvMTraceOr implements IvMFilterBuilder<IvMTrace, IvMTrace, IvMFilterGui> {

	@Override
	public String toString() {
		return "or";
	}

	@Override
	public String toString(IvMFilterGui panel) {
		return "or";
	}

	@Override
	public IvMFilterGui createGui(Runnable onUpdate, IvMDecoratorI decorator) {
		IvMFilterGui result = new IvMFilterGui(null, decorator) {
			private static final long serialVersionUID = 110211772022409817L;

			protected void setForegroundRecursively(Color colour) {

			}
		};
		result.add(result.createExplanation("Include traces that pass any of the sub-filters."));
		return result;
	}

	@Override
	public IvMFilterTreeNode<IvMTrace> buildFilter(IvMFilterGui panel) {
		return new IvMFilterTreeNodeCompositeAbstract<IvMTrace, IvMTrace>() {

			private static final long serialVersionUID = -2705606899973613204L;

			public boolean staysInLogA(IvMTrace x) {
				for (IvMFilterTreeNode<IvMTrace> child : this) {
					if (child.staysInLog(x)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public String getPrefix() {
				return "any of";
			}

			public String getDivider() {
				return "or";
			}

			public boolean couldSomethingBeFiltered() {
				for (IvMFilterTreeNode<IvMTrace> child : this) {
					if (!child.couldSomethingBeFiltered()) {
						return false;
					}
				}
				return true;
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
	public void setAttributesInfo(IvMAttributesInfo attributesInfo, IvMFilterGui gui) {

	}

}

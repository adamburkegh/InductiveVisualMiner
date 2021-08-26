package org.processmining.plugins.inductiveVisualMiner.dataanalysis;

import java.util.List;

import org.processmining.plugins.inductiveVisualMiner.chain.DataChainLinkComputationAbstract;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMCanceller;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObject;
import org.processmining.plugins.inductiveVisualMiner.chain.IvMObjectValues;

/**
 * This class computes and visualises DataRowBlocks. If you do not want to
 * compute them, use DataRowBlockAbstract.
 * 
 * @author sander
 *
 * @param <C>
 * @param <P>
 */
public abstract class DataRowBlockComputer<C, P> extends DataChainLinkComputationAbstract<C> {

	@SuppressWarnings("rawtypes")
	private final IvMObject<List> outputObject = new IvMObject<List>(getName(), List.class);

	public String getStatusBusyMessage() {
		return "computing " + getName();
	}

	public abstract List<DataRow> compute(C configuration, IvMObjectValues inputs, IvMCanceller canceller)
			throws Exception;

	public IvMObject<?>[] createOutputObjects() {
		return new IvMObject<?>[] { getOutputObject() };
	}

	public IvMObjectValues execute(C configuration, IvMObjectValues inputs, IvMCanceller canceller) throws Exception {
		return new IvMObjectValues().s(outputObject, compute(configuration, inputs, canceller));
	}

	@SuppressWarnings("rawtypes")
	public IvMObject<List> getOutputObject() {
		return outputObject;
	}

	public abstract class DataRowBlockComputerRowBlock extends DataRowBlockAbstract<C, P> {

		public DataRowBlockComputerRowBlock(DataTable<C, P> table) {
			super(table);
		}

		public DataRowBlockComputer<C, P> getComputer() {
			return DataRowBlockComputer.this;
		}

	}

	public DataRowBlockComputerRowBlock createDataRowBlock(DataTable<C, P> table) {
		return new DataRowBlockComputerRowBlock(table) {

			public String getName() {
				return DataRowBlockComputer.this.getName() + "-vis";
			}

			@SuppressWarnings("unchecked")
			public List<DataRow> gather(IvMObjectValues inputs) {
				return inputs.get(outputObject);
			}

			public IvMObject<?>[] createInputObjects() {
				return new IvMObject<?>[] { outputObject };
			}
		};
	}
}
package org.processmining.plugins.inductiveVisualMiner.dataanalysis.traceattributes;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataAnalysesView;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRowBlock;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataRowBlockComputer;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataTab;
import org.processmining.plugins.inductiveVisualMiner.dataanalysis.DataTable;

public class TraceDataTab<C, P> implements DataTab<C, P> {

	public static final String name = "Trace attributes";
	public static final String explanation = "Attributes at the trace level.\nIf traces are highlighted, attributes will be shown for highlighted and non-highlighted traces.";

	@Override
	public DataTable<C, P> createTable(DataAnalysesView<C, P> dataAnalysesView) {
		DataTable<C, P> table = new DataTable<>(name, dataAnalysesView);
		table.getModel().setColumnNames(new String[][] { {}, { "" }, { "", "" }, { "Attribute", "property", "value" },
				{ "Attribute", "property", "highlighted traces", "not-highlighted traces" } });
		return table;
	}

	@Override
	public List<DataRowBlock<C, P>> createRowBlocks(DataTable<C, P> table) {
		List<DataRowBlock<C, P>> result = new ArrayList<>();
		return result;
	}

	@Override
	public List<DataRowBlockComputer<C, P>> createRowBlockComputers() {
		List<DataRowBlockComputer<C, P>> result = new ArrayList<>();
		result.add(new TraceDataRowBlock<C, P>());
		result.add(new TraceDataRowBlockType<C, P>());
		return result;
	}

	@Override
	public String getAnalysisName() {
		return name;
	}

	@Override
	public String getExplanation() {
		return explanation;
	}

	@Override
	public boolean isSwitchable() {
		return false;
	}
}

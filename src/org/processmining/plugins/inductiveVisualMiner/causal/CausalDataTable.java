package org.processmining.plugins.inductiveVisualMiner.causal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * In a row, a NO_VALUE means that the choice was not encountered. A negative
 * value means that the ~corresponding choice was encountered, but the node was
 * skipped.
 * 
 * @author sander
 *
 */
public class CausalDataTable {
	//result variables
	private final List<Choice> columns;
	private final List<int[]> rows = new ArrayList<>();

	public static final int NO_VALUE = Integer.MIN_VALUE;

	public CausalDataTable(List<Choice> columns) {
		this.columns = columns;
	}

	public static int getSkipNode(int node) {
		return ~node;
	}

	public void addRow(int[] currentRow) {
		rows.add(currentRow);
	}

	public List<int[]> getRows() {
		return rows;
	}

	public List<Choice> getColumns() {
		return Collections.unmodifiableList(columns);
	}

	public String toString(int limit) {
		StringBuilder result = new StringBuilder();

		//header
		for (Iterator<Choice> it = columns.iterator(); it.hasNext();) {
			result.append(it.next());
			if (it.hasNext()) {
				result.append(",");
			}
		}
		result.append("\n");

		//data
		if (limit < 0) {
			limit = Integer.MAX_VALUE;
		}
		for (int i = 0; i < limit && i < rows.size(); i++) {
			for (int j = 0; j < columns.size(); j++) {
				result.append(rows.get(i)[j]);
				if (j < columns.size() - 1) {
					result.append(",");
				}
			}
			if (i < limit - 1 && i < rows.size() - 1) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	@Override
	public String toString() {
		return toString(10);
	}
}
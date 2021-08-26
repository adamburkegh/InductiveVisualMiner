package org.processmining.plugins.inductiveVisualMiner.dataanalysis;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public class DataTableModel<C, P> extends AbstractTableModel {

	private static final long serialVersionUID = -2757978513910385049L;

	private List<DataRowBlock<C, P>> blocks;
	private String[][] columnNames;
	private final String tabName;
	private final DataAnalysesView<C, P> dataAnalysesView;

	public DataTableModel(String tabName, DataAnalysesView<C, P> dataAnalysesView) {
		this.tabName = tabName;
		this.dataAnalysesView = dataAnalysesView;
	}

	public int getNumberOfNameColumns() {
		if (getBlocks() == null) {
			return 0;
		}

		int columnsNames = 0;
		for (DataRowBlock<?, ?> block : getBlocks()) {
			columnsNames = Math.max(columnsNames, block.getMaxNumberOfNames());
		}
		return columnsNames;
	}

	@Override
	public int getColumnCount() {
		if (getBlocks() == null) {
			return 0;
		}

		int columnsNames = 0;
		int columnsValues = 0;
		for (DataRowBlock<?, ?> block : getBlocks()) {
			for (int row = 0; row < block.getNumberOfRows(); row++) {
				columnsNames = Math.max(columnsNames, block.getRow(row).getNumberOfNames());
				columnsValues = Math.max(columnsValues, block.getRow(row).getNumberOfValues());
			}
		}
		return columnsNames + columnsValues;
	}

	@Override
	public String getColumnName(int column) {
		int columnCount = getColumnCount();
		if (columnNames != null && columnCount < columnNames.length && columnNames[columnCount] != null
				&& column < columnNames[columnCount].length) {
			return columnNames[columnCount][column];
		}
		return "";
	}

	@Override
	public int getRowCount() {
		if (getBlocks() == null) {
			return 0;
		}
		int rows = 0;
		for (DataRowBlock<?, ?> block : getBlocks()) {
			rows += block.getNumberOfRows();
		}
		return rows;
	}

	@Override
	public Object getValueAt(int row, int column) {
		int block = 0;
		while (row >= getBlocks().get(block).getNumberOfRows()) {
			row -= getBlocks().get(block).getNumberOfRows();
			block++;
		}

		DataRow dataRow = getBlocks().get(block).getRow(row);
		int nameColumns = getNumberOfNameColumns();
		if (column < nameColumns) {
			//attribute name
			if (column < dataRow.getNumberOfNames()) {
				return dataRow.getName(column);
			} else {
				return "";
			}
		} else {
			//attribute value
			if (column - nameColumns < dataRow.getNumberOfValues()) {
				return dataRow.getValue(column - nameColumns);
			} else {
				return DisplayType.literal("");
			}
		}
	}

	public List<DataRowBlock<C, P>> getBlocks() {
		return blocks;
	}

	public void setBlocks(List<DataRowBlock<C, P>> blocks) {
		this.blocks = blocks;
		fireTableStructureChanged();
	}

	/**
	 * Notify the table model that the rows might have changed.
	 */
	public void rowsChanged() {
		mightEnable();
		fireTableDataChanged();
	}

	/**
	 * Notify the table model that the columns might have changed. The caller
	 * must re-enable sorting on the table by calling setSorting().
	 */
	public void columnsChanged() {
		mightEnable();
		fireTableStructureChanged();
	}

	/*
	 * We might need to tell the dataAnalysesView that we are not empty anymore
	 * (this is a violation of the separation of concerns).
	 */
	private void mightEnable() {
		if (blocks != null && dataAnalysesView != null) {
			for (DataRowBlock<C, P> block : getBlocks()) {
				if (block.showsSomething()) {
					dataAnalysesView.getOnOffPanel(tabName).on();
					return;
				}
			}

			dataAnalysesView.getOnOffPanel(tabName).off();
		}
	}

	public String[][] getColumnNames() {
		return columnNames;
	}

	/**
	 * Chooses the array index of the number of columns: ColumnNames[0] =
	 * String[0]; ColumnNames[1] = String[1] { columnA }; ColumnNames[2] =
	 * String[2] { columnA, columnB }
	 * 
	 * @param columnNames
	 */
	public void setColumnNames(String[][] columnNames) {
		this.columnNames = columnNames;
	}
}
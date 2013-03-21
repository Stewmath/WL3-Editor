package record;

import javax.swing.*;

import javax.swing.table.AbstractTableModel;

public class RecordViewer extends JFrame {
	RomReader rom;

	public RecordViewer(RomReader rom) {
		super();
		this.rom = rom;

		for (int i=0; i<rom.moveableDataRecords.size(); i++) {
		}
		AbstractTableModel tableModel = new AbstractTableModel() {
			public String getColumnName(int col) {
				return columnNames[col].toString();
			}
			public int getRowCount() { return rowData.length; }
			public int getColumnCount() { return columnNames.length; }
			public Object getValueAt(int row, int col) {
				return rowData[row][col];
			}
			public boolean isCellEditable(int row, int col)
			{ return true; }
			public void setValueAt(Object value, int row, int col) {
				rowData[row][col] = value;
				fireTableCellUpdated(row, col);
			}
		};

	}
}

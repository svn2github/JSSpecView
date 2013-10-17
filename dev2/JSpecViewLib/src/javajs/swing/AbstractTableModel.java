package javajs.swing;

import javajs.lang.SB;
import javajs.util.BitSet;


abstract public interface AbstractTableModel extends TableColumn {

	TableColumn getColumn(int i);

	void toHTML(SB sb, String id, BitSet bsSelectedRows);

}

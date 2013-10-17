package javajs.swing;

import javajs.lang.StringBuffer;
import javajs.util.BitSet;


abstract public interface AbstractTableModel extends TableColumn {

	TableColumn getColumn(int i);

	void toHTML(StringBuffer sb, String id, BitSet bsSelectedRows);

}

package javajs.swing;

import javajs.lang.StringBuilder;
import javajs.util.BitSet;


abstract public interface AbstractTableModel extends TableColumn {

	TableColumn getColumn(int i);

	void toHTML(StringBuilder sb, String id, BitSet bsSelectedRows);

}

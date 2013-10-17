package javajs.swing;

import javajs.lang.SB;
import javajs.util.BS;


abstract public interface AbstractTableModel extends TableColumn {

	TableColumn getColumn(int i);

	void toHTML(SB sb, String id, BS bsSelectedRows);

}

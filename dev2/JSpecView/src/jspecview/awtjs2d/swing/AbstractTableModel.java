package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

abstract public interface AbstractTableModel extends TableColumn {

	TableColumn getColumn(int i);

	void toHTML(SB sb, String id);

}

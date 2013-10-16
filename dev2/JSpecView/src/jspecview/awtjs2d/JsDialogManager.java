package jspecview.awtjs2d;

import jspecview.awtjs2d.swing.JTextPane;
import jspecview.awtjs2d.swing.Dimension;
import jspecview.awtjs2d.swing.JDialog;
import jspecview.awtjs2d.swing.JScrollPane;
import jspecview.awtjs2d.swing.JTable;

import jspecview.api.JSVPanel;
import jspecview.api.PlatformDialog;
import jspecview.dialog.JSVDialog;
import jspecview.dialog.DialogManager;
import jspecview.source.JDXSpectrum;

/**
 * A DialogManager for JavaScript. 
 * 
 * @author hansonr
 *
 */
public class JsDialogManager extends DialogManager {

	public JsDialogManager() {
		// for reflection
	}

	@Override
	public PlatformDialog getDialog(JSVDialog jsvDialog) {
		return new JsDialog(this, jsvDialog, registerDialog(jsvDialog));
	}

	@Override
	public String getDialogInput(Object parentComponent, String phrase,
			String title, int msgType, Object icon, Object[] objects,
			String defaultStr) {
		/**
		 * @j2sNative
		 * 
		 * return prompt(phrase, defaultStr);
		 */
		{
		return null;
		}
	}

	@Override
	public void showMessageDialog(Object parentComponent, String msg,
			String title, int msgType) {
		/**
		 * @j2sNative
		 * 
		 *            alert(msg);
		 */
		{

		}
	}

	@Override
	public int[] getLocationOnScreen(Object component) {
		// TODO Auto-generated method stub
		return new int[2];
	}

	@Override
	public int getOptionFromDialog(Object frame, String[] items, JSVPanel jsvp,
			String dialogName, String labelName) {
		// for export only
		return 0;
	}

	/**
	 * Looks a lot like Swing, right? :)
	 * 
	 */
	@Override
	public void showProperties(Object frame, JDXSpectrum spectrum) {
		JDialog dialog = new JDialog();
		dialog.setTitle("Header Information");
		Object[][] rowData = spectrum.getHeaderRowDataAsArray();
		String[] columnNames = { "Label", "Description" };
		DialogTableModel tableModel = new DialogTableModel(columnNames, rowData,
				false, true);
		JTable table = new JTable(tableModel);
		table.setPreferredScrollableViewportSize(new Dimension(400, 195));
		JScrollPane scrollPane = new JScrollPane(table);
		dialog.getContentPane().add(scrollPane);
		dialog.pack();
		dialog.setVisible(true);
	}

	@Override
	public void showScrollingText(Object frame, String title, String text) {
		JDialog dialog = new JDialog();
		dialog.setTitle(title);
		JTextPane pane = new JTextPane();
		pane.getDocument().insertString(0, text, null);
		dialog.getContentPane().add(pane);
		dialog.pack();
		dialog.setVisible(true);
	}
}

package jspecview.awtjs2d;

import org.jmol.util.Parser;

import javajs.awt.Dimension;
import javajs.swing.BorderLayout;
import javajs.swing.JEditorPane;
import javajs.swing.JDialog;
import javajs.swing.JPanel;
import javajs.swing.JScrollPane;
import javajs.swing.JTable;

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
	protected void showScrollingText(Object frame, String title, String text) {
		JDialog dialog = new JDialog();
		JEditorPane sourcePane = new JEditorPane();
		sourcePane.setText(text);
		//sourcePane.setEditable(false);
		//sourcePane.setFont(new Font(null, Font.BOLD, 12));
		JScrollPane scrollPane = new JScrollPane(sourcePane);
		scrollPane.setPreferredSize(new Dimension(500, 400));
		scrollPane.setMinimumSize(new Dimension(500, 400));
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(scrollPane, BorderLayout.CENTER);
		dialog.getContentPane().add(contentPanel);
		dialog.pack();
		dialog.setVisible(true);

		dialog.setTitle(title);
		JEditorPane pane = new JEditorPane();
		pane.setText(text);
		dialog.getContentPane().add(pane);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	/**
	 * Jmol.Dialog.click() callback
	 * @param eventId 
	 */
	public void actionPerformed(String eventId) {
		if (eventId.indexOf("/JT") >= 0) {
			int pt = eventId.indexOf("_");
			int irow = Parser.parseInt(eventId.substring(pt + 1));
			int icol = Parser.parseInt(eventId.substring(eventId.indexOf("_", pt + 1) + 1));
			processTableEvent(eventId, irow, icol, false);
			return;
		}
		processClick(eventId);
	}

}

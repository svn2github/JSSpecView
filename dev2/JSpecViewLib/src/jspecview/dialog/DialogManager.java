package jspecview.dialog;

import java.util.Hashtable;
import java.util.Map;

import jspecview.api.JSVPanel;
import jspecview.api.PlatformDialog;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.source.JDXSource;
import jspecview.source.JDXSpectrum;

abstract public class DialogManager {

	protected JSViewer viewer;
	private Map<Object, String> htSelectors;
	protected Map<String, JSVDialog> htDialogs;
	private int dialogCount;

	public DialogManager set(JSViewer viewer) {
		this.viewer = viewer;
		htSelectors = new Hashtable<Object, String>();
		htDialogs = new Hashtable<String, JSVDialog>();
		return this;
	}

	public final static int PLAIN_MESSAGE       = -1; // JOptionPane.PLAIN_MESSAGE
	public final static int ERROR_MESSAGE       =  0; // JOptionPane.ERROR_MESSAGE
	public final static int INFORMATION_MESSAGE =  1; // JOptionPane.INFORMATION_MESSAGE
	public final static int WARNING_MESSAGE     =  2; // JOptionPane.WARNING_MESSAGE
	public final static int QUESTION_MESSAGE    =  3; // JOptionPane.QUESTION_MESSAGE

	abstract public PlatformDialog getDialog(JSVDialog jsvDialog, DialogParams params);

	abstract public String getDialogInput(Object parentComponent, String phrase,
			String title, int msgType, Object icon, Object[] objects,
			String defaultStr);

	abstract public int[] getLocationOnScreen(Object component);

	abstract public int getOptionFromDialog(Object frame, String[] items, JSVPanel jsvp,
			String dialogName, String labelName);

	abstract public void showMessageDialog(Object parentComponent, String msg, String title, int msgType);

  abstract public void showProperties(Object frame, JDXSpectrum spectrum);
  
  abstract public void showText(Object frame, String title, String text);

  public void registerSelector(String selectorName, Object columnSelector) {
		htSelectors.put(columnSelector, selectorName);
	}

	protected String getSelectorName(Object selector) {
		return htSelectors.get(selector);
	}

	protected String registerDialog(JSVDialog jsvDialog, String key) {
		String id = key + " " + (++dialogCount); 
		htDialogs.put(id, jsvDialog);
		return id;
	}

	public String getField(String url, String name) {
		url += "&";
		String key = "&" + name + "=";
		int pt = url.indexOf(key);
		return (pt < 0 ? null : url.substring(pt + key.length(), url.indexOf("&",
				pt + 1)));
	}

	public void showSourceErrors(Object frame, JDXSource currentSource) {
		if (currentSource == null) {
			showMessageDialog(frame,
					"Please Select a Spectrum.", "Select Spectrum", WARNING_MESSAGE);
			return;
		}
		String errorLog = currentSource.getErrorLog();
		if (errorLog != null && errorLog.length() > 0)
			showText(frame, currentSource.getFilePath(), errorLog);
		else
			showMessageDialog(frame, "No errors found.",
					"Error Log", INFORMATION_MESSAGE);
	}

	public void showSource(Object frame, JDXSource currentSource) {
		if (currentSource == null) {
			showMessageDialog(frame, "Please Select a Spectrum", "Select Spectrum",
					WARNING_MESSAGE);
			return;
		}
		try {
			String f = currentSource.getFilePath();
			showText(null, f, JSVFileManager.getFileAsString(f, null));
		} catch (Exception ex) {
			showMessageDialog(frame, "File Not Found", "SHOWSOURCE", ERROR_MESSAGE);
		}
	}

	/**
	 * processing click event from platform DialogManager
	 * 
	 * @param eventId   dialogId/buttonId starting with "btn", "chk", "cmb", or "txt"
	 */
	
	protected void processClick(String eventId) {
		int pt = eventId.lastIndexOf("/");
		String id = eventId.substring(pt + 1);
		String dialog = eventId.substring(0, pt);
		dialogCallback(dialog, id, null);
	}

	/**
	 * processing table cell click event from platform DialogManager; takes two
	 * hits in AWT -- one a row, the other a column
	 * 
	 * @param eventId
	 *          dialogId/[ROW|COL] or just dialogId
	 * @param index1
	 *          row if just dialogId or (row or col if AWT)
	 * @param index2
	 *          column if just dialogId or -1 if AWT
	 * @param adjusting
	 */
	protected void processTableEvent(String eventId, int index1, int index2,
			boolean adjusting) {
		int pt = eventId.lastIndexOf("/");
		String dialog = eventId.substring(0, pt);
		String selector = eventId.substring(pt + 1);
		String msg = "&selector=" + selector + "&index=" + index1
				+ (index2 < 0 ? "&adjusting=" + adjusting : "&index2=" + index2);
		dialogCallback(dialog, "tableSelect", msg);
	}

	/**
	 * processing window closing event from platform DialogManager
	 * 
	 * @param dialog
	 */
	protected void processWindowClosing(String dialog) {
		dialogCallback(dialog, "windowClosing", null);
	}

	/**
	 * Send the callback to the appropriate dialog
	 * 
	 * @param dialogId
	 * @param id
	 * @param msg
	 */
	private void dialogCallback(String dialogId, String id, String msg) {
		JSVDialog jsvDialog = htDialogs.get(dialogId);
		if (jsvDialog != null)
			jsvDialog.callback(id, msg);		
	}

}

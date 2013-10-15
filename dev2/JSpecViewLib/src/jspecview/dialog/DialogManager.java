package jspecview.dialog;

import java.util.Hashtable;
import java.util.Map;

import jspecview.api.JSVPanel;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.source.JDXSource;

abstract public class DialogManager {

	protected JSViewer viewer;
	private Map<Object, String> htSelectors;
	private Map<String, AnnotationDialog> htDialogs;
	private int dialogCount;

	public DialogManager set(JSViewer viewer) {
		this.viewer = viewer;
		htSelectors = new Hashtable<Object, String>();
		htDialogs = new Hashtable<String, AnnotationDialog>();
		return this;
	}

	public final static int PLAIN_MESSAGE       = -1; // JOptionPane.PLAIN_MESSAGE
	public final static int ERROR_MESSAGE       =  0; // JOptionPane.ERROR_MESSAGE
	public final static int INFORMATION_MESSAGE =  1; // JOptionPane.INFORMATION_MESSAGE
	public final static int WARNING_MESSAGE     =  2; // JOptionPane.WARNING_MESSAGE
	public final static int QUESTION_MESSAGE    =  3; // JOptionPane.QUESTION_MESSAGE

	abstract public PlatformDialog getDialog(AnnotationDialog jsvDialog, DialogParams params);

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

	protected String registerDialog(AnnotationDialog jsvDialog, String key) {
		String id = key + " " + (++dialogCount); 
		htDialogs.put(id, jsvDialog);
		return id;
	}

	protected void runScript(String dialog, String id) {
		viewer.runScript("event://" + dialog + "?&id=" + id);
	}

	public String getField(String url, String name) {
		url += "&";
		String key = "&" + name + "=";
		int pt = url.indexOf(key);
		return (pt < 0 ? null : url.substring(pt + key.length(), url.indexOf("&",
				pt + 1)));
	}

	public boolean dialogCallback(String url) {
		String dialogID = url.substring(8, url.indexOf("?"));
		GenericDialog jsvDialog = htDialogs.get(dialogID);
		return (jsvDialog == null ? false : jsvDialog.callback(getField(url, "id"), url));		
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


}

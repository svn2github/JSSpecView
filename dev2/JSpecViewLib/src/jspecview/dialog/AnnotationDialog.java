package jspecview.dialog;

import jspecview.api.AnnotationData;
import jspecview.api.JSVPanel;
import jspecview.dialog.DialogParams;
import jspecview.common.Annotation;
import jspecview.common.Coordinate;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSViewer;
import jspecview.common.MeasurementData;
import jspecview.common.Parameters;

abstract public class AnnotationDialog extends Annotation implements AnnotationData {

	abstract public int[] getPosXY();

	protected AType type;
	protected String title;
	protected JSViewer viewer;
	protected JDXSpectrum spec;

	protected DialogManager manager;
	protected DialogParams dialogParams;
	protected PlatformDialog dialog;

	private int[] loc;

	protected Object txt1;
	protected Object txt2;
	private Object showHideButton; // text changeable
	protected Object combo1; // measurement listing, peaks
	
	private boolean addClearBtn, addCombo1;
	private boolean isNumeric = true; // not Views or OverlayLegend
	private boolean defaultVisible = true; // not OverlayLegend

	abstract protected void addUniqueControls();

	abstract public boolean callback(String id, String msg);

	public AnnotationDialog setParams(String title, JSViewer viewer, JDXSpectrum spec) {
		this.title = title;
		this.viewer = viewer;
		this.spec = spec;
		manager = viewer.getDialogManager();
		switch (type) {
		case Integration:
			addClearBtn = true;
			break;
		case Measurements:
			addCombo1 = true;
			break;
		case NONE:
			break;
		case OverlayLegend:
			isNumeric = false;
			defaultVisible = false;
			break;
		case PeakList:
			addClearBtn = true;
			break;
		case Views:
			isNumeric = false;
			break;
		}
		dialogParams = new DialogParams(type, this, viewer, spec, ((Parameters) viewer
				.getAwtInterface("Parameters")).setName("dialogParams"));
		dialogParams.setup();
		initDialog();
		return this;
	}
	
	/**
	 * 
	 * @param panel
	 *          a PPanel (Applet) or a JScrollPane (MainFrame)
	 * 
	 * @param posXY
	 *          static for a given dialog
	 */
	public void restoreDialogPosition(JSVPanel panel, int[] posXY) {
		if (panel != null) {
			if (posXY[0] == Integer.MIN_VALUE) {
				posXY[0] = 0;
				posXY[1] = -20;
			}
			int[] pt = manager.getLocationOnScreen(panel);
			int height = panel.getHeight();
			loc = new int[] { Math.max(0, pt[0] + posXY[0]), Math.max(0, pt[1] + height + posXY[1]) };
			dialog.setIntLocation(loc);
		}
	}
	
	public void saveDialogPosition(int[] posXY) {
		try {
			int[] pt = manager.getLocationOnScreen(dialog);
			posXY[0] += pt[0] - loc[0];
			posXY[1] += pt[1] - loc[1];
		} catch (Exception e) {
			// ignore
		}
	}
	
	// //// frame construction ////////

	private void initDialog() {
		dialog = manager.getDialog(this, dialogParams);
		restoreDialogPosition(dialogParams.jsvp, getPosXY());
		dialog.setTitle(title);
		layoutDialog();
	}

	protected void layoutDialog() {
		dialog.startLayout();
		addUniqueControls();
		if (isNumeric ) {
			dialogParams.getUnitOptions();
			if (addCombo1)
				combo1 = dialog.addSelectOption("cmbUnits", "Units", 
						dialogParams.unitOptions, dialogParams.unitPtr.intValue(),
						dialogParams.addUnits);
			// txtFontSize = ((DialogHelper dialogHelper)).addInputOption("FontSize",
			// "Font Size", null, null, "10");

			dialog.addButton("btnApply", "Apply");
			showHideButton = dialog.addButton("btnShow", "Show");
			if (addClearBtn)
				dialog.addButton("btnClear", "Clear");
		}
		dialog.endLayout();
		checkEnables();
		dialog.setVisible(defaultVisible);
	}

	/**
	 * @param id
	 * @param msg
	 * @return true if consumed
	 */
	public boolean callbackAD(String id, String msg) {
		if (id.equals("tableSelect")) {
			dialogParams.tableSelect(manager, msg);
		} else if (id.equals("btnClear")) {
			dialogParams.clear();
		} else if (id.equals("btnApply")) {
			dialogParams.eventApply();
		} else if (id.equals("btnShow")) {
			String label = dialog.getText(showHideButton);
			dialogParams.eventShowHide(label.equals("Show"));
		} else if (id.equals("btnUnits")) {
			dialogParams.setPrecision(dialog.getSelectedIndex(combo1));
		} else if (id.startsWith("txt")) {
			dialogParams.eventApply();
		} else if (id.equals("windowClosing")) {
			dialogParams.done();
			return true;
		}

		return true;
	}

	/**
	 * @param dialogHelper
	 */
	protected void addUniqueControls(DialogManager dialogHelper) {
		// int and peak only
	}

	// /////// general interface to the outside world ////////

	public AType getAType() {
		return dialogParams.thisType;
	}

	public void setSpecShift(double dx) {
		dialogParams.setSpecShift(dx);
	}

	public MeasurementData getData() {
		return dialogParams.getData();
	}

	public void setFields() {
		dialogParams.setFields();
	}

	public String getKey() {
		return dialogParams.key;
	}

	public void setKey(String key) {
		dialogParams.key = key;
	}

	public JDXSpectrum getSpectrum() {
		return dialogParams.spec;
	}

	public boolean getState() {
		return dialogParams.isON;
	}

	public void setState(boolean b) {
		dialogParams.isON = b;
	}

	public void update(Coordinate clicked) {
		dialogParams.update(clicked);
	}

	// ////// interface to DialogParams////////

	public void checkEnables() {
		boolean isShow = dialogParams.checkVisible();
			dialog.setText(showHideButton, isShow ? "Hide" : "Show");
	}

	public void createTable(Object[][] data, String[] header, int[] widths) {
		dialogParams.tableData = data;
		dialog.createTable(data, header, widths);
	}

	public void setTableSelectionEnabled(boolean enabled) {
		dialog.setCellSelectionEnabled(enabled);
	}

	public void setTableSelectionInterval(int i, int j) {
		dialog.setSelectionInterval(i, j);
	}

	public Parameters getParameters() {
		return dialogParams.myParams;
	}

	public void showMessage(String msg, String title, int msgType) {
		manager.showMessageDialog(dialog, msg, title, msgType);
	}

	public void setThreshold(double y) {
		dialog.setText(txt1, dialogParams.getThreasholdText(y));
	}

	public void setComboSelected(int i) {
		dialog.setSelectedIndex(combo1, i);
	}

	public void applyFromFields() {
		dialogParams.apply(null);
	}

	public void setParamsFromFields() {
		dialogParams.setParams(null);
	}

	public void loadDataFromFields() {
		dialogParams.loadData(null);
	}

	public AnnotationDialog reEnable() {
		dialogParams.reEnable();
		return this;
	}
	
	public void setData(AnnotationData data) {
		dialogParams.setData(data);
	}

	public void dispose() {
		dialog.dispose();		
	}

	public void setVisible(boolean visible) {
		dialog.setVisible(visible);		
	}

	public boolean isVisible() {
		return dialog.isVisible();
	}

}

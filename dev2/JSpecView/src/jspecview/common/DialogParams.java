package jspecview.common;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.Txt;

import jspecview.api.ScriptInterface;
import jspecview.common.Annotation.AType;

/**
 * DialogParams handles all the platform-independent 
 * needs of AwtAnnotationDialog and subclasses of it. 
 * 
 * 
 * @author hansonr
 *
 */
public class DialogParams {

	public AnnotationDialog dialog;

	public AType thisType;
	public String subType;

	public ScriptInterface si;
	public JSVPanel jsvp;
	public JDXSpectrum spec;

	public String thisKey;

	public Parameters myParams;
	public Map<String, Object> options;

	public MeasurementData xyData;
	public String key;

	public String[][] tableData;
	public boolean addUnits;

	public Object[] myOptions;
	public String[] unitOptions;
	public int[] formatOptions;

	public Integer unitPtr;
	public int precision = 1;

	public boolean disposeOnDone;

	public boolean isON = true;


	public DialogParams(AType type, AnnotationDialog dialog, ScriptInterface si,
			JDXSpectrum spec, Parameters myParams) {
		this.thisType = type;
		this.dialog = dialog;
		this.si = si;
		jsvp = si.getSelectedPanel();
		this.spec = spec;
		this.myParams = myParams;
		options = new Hashtable<String, Object>();
		dialog.restoreDialogPosition(jsvp, dialog.getPosXY());
	}

	public String getThreasholdText(double y) {
		if (Double.isNaN(y)) {
			PanelData pd = jsvp.getPanelData();
			double f = (pd.getSpectrum().isInverted() ? 0.1 : 0.9);
			Coordinate c = pd.getClickedCoordinate();
			y = (c == null ? (pd.getView().minYOnScale * f + pd.getView().maxYOnScale)
					* (1 - f)
					: c.getYVal());
		}
		String sy = Txt.formatDecimalDbl(y, y < 1000 ? 2 : -2); // "#0.00" :
																														// "0.00E0"
		return " " + sy;
	}

	public MeasurementData getPeakData() {
		PeakData md = new PeakData(AType.PeakList, spec);
		md.setPeakList(myParams, precision, jsvp.getPanelData().getView());
		xyData = md;
		return null;
	}

	public void setup() {
		subType = spec.getTypeLabel();
		thisKey = thisType + "_" + subType;
		myOptions = (Object[]) options.get(thisKey);
		options = new Hashtable<String, Object>();
		if (myOptions == null)
			options.put(thisKey, myOptions = spec.getDefaultAnnotationInfo(thisType));
		unitOptions = (String[]) myOptions[0];
		formatOptions = (int[]) myOptions[1];
		unitPtr = (Integer) options.get(thisKey + "_unitPtr");
		if (unitPtr == null)
			unitPtr = (Integer) myOptions[2];
	}

	public boolean checkVisible() {
		return si.getSelectedPanel().getPanelData().getShowAnnotation(thisType);
	}

	public void getUnitOptions() {
		String key = thisKey + "_format";
		Integer format = (Integer) options.get(key);
		if (format == null)
			options.put(key, format = Integer
					.valueOf(formatOptions[unitPtr == null ? 0 : unitPtr.intValue()]));
		// txtFormat = dialogHelper.addInputOption("numberFormat", "Number Format",
		// format, null, null, false);
	}
	
	public boolean skipCreate;

	public void eventApply() {
		switch (thisType) {
		case Integration:
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			createData();
			skipCreate = true;
			break;		
		}
		dialog.applyFromFields();
	}

	public void eventShowHide(boolean isShow) {
		isON = isShow;
		if (isShow)
			eventApply();
		jsvp.doRepaint();
		dialog.checkEnables();
	}
	
	public void apply(Object[] objects) {
		try {
			switch (thisType) {
			case Integration:
				double offset = Double.parseDouble((String) objects[0]);
				double range = Double.parseDouble((String) objects[1]);
				myParams.integralOffset = offset;
				myParams.integralRange = range;
				myParams.integralDrawAll = false;// chkResets.isSelected();
				((IntegralData) getData()).update(myParams);
				jsvp.doRepaint();
				break;
			case Measurements:
				// n/a
				break;
			case NONE:
				return;
			case PeakList:
				if (!skipCreate) {
					dialog.setThreshold(Double.NaN);
					createData();
				}
				skipCreate = false;
				break;
			}
			dialog.loadDataFromFields();
			dialog.checkEnables();
			jsvp.doRepaint();
		} catch (Exception e) {
			// ignore
		}
	}

	public void clear() {
		if (xyData != null) {
			xyData.clear();
			dialog.applyFromFields();
		}
	}

	public void done() {
		if (disposeOnDone) {
			dialog.dispose();
			return;
		}
		jsvp.getPanelData().removeDialog(dialog);
		// setState(false);
		if (xyData != null)
			xyData.setState(isON);
		dialog.dispose();
		jsvp.doRepaint();
	}

	public void reEnable() {
		switch (thisType) {
		case Integration:
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			skipCreate = true;
			break;
		}
		dialog.setVisible(true);
		isON = true;
		dialog.applyFromFields();
	}

	public MeasurementData getData() {
		if (xyData == null)
			createData();
		return xyData;
	}

	public void setData(AnnotationData data) {
		myParams = data.getParameters();
		xyData = (MeasurementData) data;
	}

	public void setSpecShift(double dx) {
		if (xyData != null)
			xyData.setSpecShift(dx);
	}

	public int iRowSelected = -1;
	public int iColSelected = -1;
	public int iRowColSelected = -1;

	private int iSelected = -1;

	public void setType(AType type) {
		this.thisType = type;
		switch (type) {
		case Measurements:
			addUnits = true;
			break;
		case Integration:
			break;
		case PeakList:
			break;
		case NONE:
			break;

		}
	}

	public void tableCellSelect(int iRow, int iCol) {
		String value = tableData[iRow][1];
		int icolrow = iRowSelected * 1000 + iColSelected;
		if (icolrow == iRowColSelected)
			return;
		iRowColSelected = icolrow;
		try {
			switch (thisType) {
			case Integration:
				for (int i = 0; i < xyData.size(); i++)
					if (Txt.formatDecimalDbl(xyData.get(i).getXVal(), 2).equals(value)) {
						iSelected = i;
						jsvp.getPanelData().setXPointers(spec, xyData.get(i).getXVal(),
								spec, xyData.get(i).getXVal2());
						jsvp.doRepaint();
						break;
					}
				dialog.checkEnables();
				break;
			case Measurements:
				break;
			case NONE:
				break;
			case PeakList:
				try {
					switch (iCol) {
					case 6:
					case 5:
					case 4:
						double x1 = Double.parseDouble(value);
						double x2 = Double.parseDouble(tableData[iRow + 3 - iCol][1]);
						jsvp.getPanelData().setXPointers(spec, x1, spec, x2);
						break;
					default:
						jsvp.getPanelData().findX(spec, Double.parseDouble(value));
					}
				} catch (Exception e) {
					jsvp.getPanelData().findX(spec, 1E100);
				}
				jsvp.doRepaint();
				break;
			}
		} catch (Exception e) {
			// for parseDouble
		}
	}

	public void loadData(Object param) {
		String[][] data;
		String[] header;
		int[] widths;
		switch (thisType) {
		case Integration:
			if (xyData == null)
				createData();
			iSelected = -1;
			data = ((IntegralData) xyData).getMeasurementListArray(null);
			header = xyData.getDataHeader();
			widths = new int[] { 40, 65, 65, 50 };
			dialog.createTable(data, header, widths);
			break;
		case Measurements:
			if (xyData == null)
				return;
			data = xyData.getMeasurementListArray(param.toString());
			header = xyData.getDataHeader();
			widths = new int[] { 40, 65, 65, 50 };
			dialog.createTable(data, header, widths);
			break;
		case NONE:
			break;
		case PeakList:
			if (xyData == null)
				createData();
			data = ((PeakData)xyData).getMeasurementListArray(null);
			header = ((PeakData)xyData).getDataHeader();
			widths = new int[] {40, 65, 50, 50, 50, 50, 50};
			dialog.createTable(data, header, widths);
			dialog.setTableSelectionEnabled(true);
			break;
		}
	}

	public void update(Coordinate clicked) {
		switch (thisType) {
		case Integration:
			dialog.loadDataFromFields();
			dialog.checkEnables();
			break;
		case Measurements:
			dialog.loadDataFromFields();
			dialog.checkEnables();
			break;
		case NONE:
			break;
		case PeakList:
			dialog.applyFromFields();
			if (xyData == null || clicked == null)
				return;
			int ipt = 0;
			double dx0 = 1e100;
			double xval = clicked.getXVal();
			PeakData md = (PeakData) xyData;
			for (int i = md.size(); --i >= 0;) {
				double dx = Math.abs(xval - md.get(i).getXVal());
				if (dx < dx0) {
					dx0 = dx;
					ipt = i;
				}
				if (dx0 < 0.1)
					dialog.setTableSelectionInterval(md.size() - 2 - ipt,
							md.size() - 1 - ipt);
			}
			break;

		}
	}

	public void createData() {
		switch (thisType) {
		case Integration:
			xyData = new IntegralData(spec, myParams);
			iSelected = -1;
			break;
		case Measurements:
			// n/a
			break;
		case NONE:
			break;
		case PeakList:
			dialog.setParamsFromFields();
			PeakData md = new PeakData(AType.PeakList, spec);
		  md.setPeakList(myParams, precision, jsvp.getPanelData().getView());
			xyData = md;
			dialog.loadDataFromFields();
			break;

		}
		// TODO Auto-generated method stub

	}

	public void deleteIntegral() {
		if (!checkSelectedIntegral())
			return;
		xyData.remove(iSelected);
		iSelected = -1;
		iRowColSelected = -1;
		dialog.applyFromFields();
		jsvp.doRepaint();
	}

	public boolean checkSelectedIntegral() {
		if (iSelected < 0) {
			dialog
					.showMessage("Select a line on the table first, then click this button.");
			return false;
		}
		return true;
	}

	public double lastNorm = 1;
	public double lastMin = 0;

	public void processEvent(String what, double val) {
		switch (thisType) {
		case Integration:
			if (what.equals("min")) {
				((IntegralData) xyData).setMinimumIntegral(lastMin = val);
			} else if (what.equals("int")) {
				if (val < 0)
					return;
				((IntegralData) xyData).setSelectedIntegral(xyData.get(iSelected),
						lastNorm = val);
			}
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			break;
		}
		dialog.applyFromFields();
		jsvp.doRepaint();
	}

	public void setFields() {
		switch (thisType) {
		case Integration:
			break;
		case Measurements:
			break;
		case NONE:
			break;
		case PeakList:
			myParams = xyData.getParameters();
			dialog.setThreshold(myParams.peakListThreshold);
			dialog.setComboSelected(myParams.peakListInterpolation.equals("none") ? 1 : 0);
			createData();
			break;			
		}
	}

	public void setParams(Object[] objects) {
		try {
			switch (thisType) {
			case Integration:
				break;
			case Measurements:
				break;
			case NONE:
				break;
			case PeakList:
				double thresh = Double.parseDouble((String) objects[0]);
				myParams.peakListThreshold = thresh;
				myParams.peakListInterpolation = (String) objects[1];
				myParams.precision = precision;
				break;
			}
		} catch (Exception e) {
			// for parseDouble
		}
	}

	public void setPrecision(int i) {
		precision = formatOptions[i];
	}

}

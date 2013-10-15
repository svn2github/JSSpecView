package jspecview.dialog;

import java.util.Hashtable;
import java.util.Map;

import org.jmol.util.Parser;
import org.jmol.util.Txt;

import jspecview.api.AnnotationData;
import jspecview.api.JSVPanel;
import jspecview.common.Coordinate;
import jspecview.common.IntegralData;
import jspecview.common.JSViewer;
import jspecview.common.MeasurementData;
import jspecview.common.PanelData;
import jspecview.common.Parameters;
import jspecview.common.PeakData;
import jspecview.common.Annotation.AType;
import jspecview.source.JDXSpectrum;

/**
 * DialogParams handles all the platform-independent 
 * needs of AwtAnnotationDialog and subclasses of it. 
 * 
 * 
 * @author hansonr
 *
 */
public class DialogParams {

	public JSVDialog annDialog;

	public AType thisType;
	public String subType;

	public JSViewer viewer;
	public JSVPanel jsvp;
	public JDXSpectrum spec;

	public String thisKey;

	public Parameters myParams;
	public Map<String, Object> options;

	public MeasurementData xyData;
	public String key;

	public Object[][] tableData;
	public boolean addUnits;

	public Object[] myOptions;
	public String[] unitOptions;
	public int[] formatOptions;

	public Integer unitPtr;
	public int precision = 1;

	public boolean isON = true;


	public DialogParams(AType type, JSVDialog dialog, JSViewer viewer,
			JDXSpectrum spec, Parameters myParams) {
		this.thisType = type;
		this.annDialog = dialog;
		this.viewer = viewer;
		jsvp = viewer.selectedPanel;
		this.spec = spec;
		this.myParams = myParams;
		options = new Hashtable<String, Object>();
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
		subType = (spec == null ? "" : spec.getTypeLabel());
		thisKey = thisType + "_" + subType;
		if (spec == null)
			return;
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
		return viewer.selectedPanel.getPanelData().getShowAnnotation(thisType);
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
		case OverlayLegend:
			break;
		case Views:
			break;		
		}
		annDialog.applyFromFields();
	}

	public void eventShowHide(boolean isShow) {
		isON = isShow;
		if (isShow)
			eventApply();
		jsvp.doRepaint();
		annDialog.checkEnables();
	}
	
	public void apply(Object[] objects) {
		try {
			switch (thisType) {
			case Integration:
				double offset = Double.parseDouble((String) objects[0]);
				double scale = Double.parseDouble((String) objects[1]);
				myParams.integralOffset = offset;
				myParams.integralRange = scale;
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
					annDialog.setThreshold(Double.NaN);
					createData();
				}
				skipCreate = false;
				break;
			case OverlayLegend:
				break;
			case Views:
				break;
			}
			annDialog.loadDataFromFields();
			annDialog.checkEnables();
			jsvp.doRepaint();
		} catch (Exception e) {
			// ignore
		}
	}

	public void clear() {
		if (xyData != null) {
			xyData.clear();
			annDialog.applyFromFields();
		}
	}

	public void done() {
		if (jsvp != null && spec != null)
			jsvp.getPanelData().removeDialog(annDialog);
		// setState(false);
		if (xyData != null)
			xyData.setState(isON);
		annDialog.saveDialogPosition(annDialog.getPosXY());
		annDialog.dispose();
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
		case OverlayLegend:
			break;
		case Views:
			break;
		}
		annDialog.setVisible(true);
		isON = true;
		annDialog.applyFromFields();
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
		case OverlayLegend:
			break;
		case Views:
			break;
		case NONE:
			break;

		}
	}

	public void tableCellSelect(int iRow, int iCol) {
		Object value = tableData[iRow][1];
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
				annDialog.checkEnables();
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
						double x1 = Double.parseDouble((String) value);
						double x2 = Double.parseDouble((String) tableData[iRow + 3 - iCol][1]);
						jsvp.getPanelData().setXPointers(spec, x1, spec, x2);
						break;
					default:
						jsvp.getPanelData().findX(spec, Double.parseDouble((String) value));
					}
				} catch (Exception e) {
					jsvp.getPanelData().findX(spec, 1E100);
				}
				jsvp.doRepaint();
				break;
			case OverlayLegend:
				jsvp.getPanelData().setSpectrum(iRow, false);
				break;
			case Views:
				break;
			}
		} catch (Exception e) {
			// for parseDouble
		}
	}

	public void loadData(Object param) {
		Object[][] data;
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
			annDialog.createTable(data, header, widths);
			break;
		case Measurements:
			if (xyData == null)
				return;
			data = xyData.getMeasurementListArray(param.toString());
			header = xyData.getDataHeader();
			widths = new int[] { 40, 65, 65, 50 };
			annDialog.createTable(data, header, widths);
			break;
		case NONE:
			break;
		case PeakList:
			if (xyData == null)
				createData();
			data = ((PeakData)xyData).getMeasurementListArray(null);
			header = ((PeakData)xyData).getDataHeader();
			widths = new int[] {40, 65, 50, 50, 50, 50, 50};
			annDialog.createTable(data, header, widths);
			annDialog.setTableSelectionEnabled(true);
			break;
		case OverlayLegend:
			header = new String[] { "No.", "Plot Color", "Title" };
			data = viewer.selectedPanel.getPanelData().getOverlayLegendData();
			widths = new int[] {30, 60, 250};
			annDialog.createTable(data, header, widths);
			annDialog.setTableSelectionEnabled(true);
			break;
		case Views:
			break;
		}
	}

	public void update(Coordinate clicked) {
		switch (thisType) {
		case Integration:
			annDialog.loadDataFromFields();
			annDialog.checkEnables();
			break;
		case Measurements:
			annDialog.loadDataFromFields();
			annDialog.checkEnables();
			break;
		case NONE:
			break;
		case PeakList:
			annDialog.applyFromFields();
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
					annDialog.setTableSelectionInterval(md.size() - 2 - ipt,
							md.size() - 1 - ipt);
			}
			break;
		case OverlayLegend:
			break;
		case Views:
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
			annDialog.setParamsFromFields();
			PeakData md = new PeakData(AType.PeakList, spec);
		  md.setPeakList(myParams, precision, jsvp.getPanelData().getView());
			xyData = md;
			annDialog.loadDataFromFields();
			break;
		case OverlayLegend:
			break;
		case Views:
			break;
		}
	}

	public void deleteIntegral() {
		if (!checkSelectedIntegral())
			return;
		xyData.remove(iSelected);
		iSelected = -1;
		iRowColSelected = -1;
		annDialog.applyFromFields();
		jsvp.doRepaint();
	}

	public boolean checkSelectedIntegral() {
		if (iSelected < 0) {
			annDialog
					.showMessage("Select a line on the table first, then click this button.", "Integration", DialogManager.INFORMATION_MESSAGE);
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
		case OverlayLegend:
			break;
		case Views:
			break;
		}
		annDialog.applyFromFields();
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
			annDialog.setThreshold(myParams.peakListThreshold);
			annDialog.setComboSelected(myParams.peakListInterpolation.equals("none") ? 1 : 0);
			createData();
			break;			
		case OverlayLegend:
			break;
		case Views:
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
			case OverlayLegend:
				break;
			case Views:
				break;
			}
		} catch (Exception e) {
			// for parseDouble
		}
	}

	public void setPrecision(int i) {
		precision = formatOptions[i];
	}

	public boolean runScript(String script) {
		viewer.runScript(script);
		return true;
	}

	private int tableRCflag = 0;

	public void tableSelect(DialogManager manager, String url) {
		boolean isAdjusting = "true".equals(manager.getField(url, "adjusting"));
		boolean isColumn = "COLUMN".equals(manager.getField(url, "selector"));
		int index = Parser.parseInt(manager.getField(url, "index"));
		
		try {
			if (isAdjusting) {
				if (isColumn) {
					iColSelected = index;
					tableRCflag = 1;
				} else {
					iRowSelected = index;
					tableRCflag = 2;
				}
				return;
			}
			if (isColumn != (tableRCflag == 1))
				return;
			tableCellSelect(iRowSelected, iColSelected);
		} catch (Exception ee) {
			// ignore
		}
	}

}

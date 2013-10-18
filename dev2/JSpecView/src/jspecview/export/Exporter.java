package jspecview.export;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javajs.util.List;

import org.jmol.io.Base64;
import org.jmol.util.Txt;

import jspecview.api.ExportInterface;
import jspecview.api.JSVFileHelper;
import jspecview.api.JSVPanel;
import jspecview.common.ExportType;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;
import jspecview.common.PrintLayout;
import jspecview.common.ScriptToken;
import jspecview.common.Annotation.AType;

public class Exporter implements ExportInterface {

	public Exporter() {
		// for reflection; called directly only from MainFrame
	}
	
  /* (non-Javadoc)
	 * @see jspecview.export.ExportInterface#exportCmd(jspecview.api.JSVPanel, org.jmol.util.JmolList, boolean)
	 */
  public String exportCmd(JSVPanel jsvp, List<String> tokens, boolean forInkscape) {
    // MainFrame or applet EXPORT command
    String mode = "XY";
    String fileName = null;
    switch (tokens.size()) {
    default:
      return "EXPORT what?";
    case 1:
      fileName = Txt.trimQuotes(tokens.get(0));
      int pt = fileName.indexOf(".");
      if (pt < 0)
        return "EXPORT mode?";
      break;
    case 2:
      mode = tokens.get(0).toUpperCase();
      fileName = Txt.trimQuotes(tokens.get(1));
      break;
    }
    String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
        .toUpperCase();
    if (ext.equals("JDX")) {
      if (mode == null)
        mode = "XY";
    } else if (ExportType.isExportMode(ext)) {
      mode = ext;
    } else if (ExportType.isExportMode(mode)){
      fileName += "."  + mode;
    }
    ExportType type = ExportType.getType(mode);
    if (forInkscape && type == ExportType.SVG)
      type = ExportType.SVGI;
    return exportSpectrumOrImage(jsvp, type, -1, fileName);
  }


  /* (non-Javadoc)
	 * @see jspecview.export.ExportInterface#exportTheSpectrum(java.lang.String, java.lang.String, jspecview.common.JDXSpectrum, int, int)
	 */
  public String exportTheSpectrum(String type, String path, 
                                         JDXSpectrum spec, int startIndex, int endIndex)
      throws IOException {
  	ExportType mode = ExportType.getType(type);
    switch (mode) {
    case XY:
    case DIF:
    case DIFDUP:
    case FIX:
    case PAC:
    case SQZ:
      return JDXExporter.export(mode, path, spec, startIndex, endIndex);      
    case SVG:
    case SVGI:
      return (new SVGExporter()).exportAsSVG(path, spec, startIndex, endIndex, mode == ExportType.SVGI);
    case CML:
      return (new CMLExporter()).exportAsCML(path, spec, startIndex, endIndex);
    case AML:
      return (new AMLExporter()).exportAsAnIML(path, spec, startIndex, endIndex);
    default:
      return null;
    }
  }

	/** 
	 * 
	 * Application only
	 * 
	 */
	public void exportSpectrum(JSViewer viewer, String type) {
		JSVPanel jsvp = viewer.selectedPanel;
		if (jsvp == null)
			return;
		ExportType eType = ExportType.getType(type);
		switch (eType) {
		case PDF:
			printPDF(viewer, "PDF");
			break;
		case PNG:
		case JPG:
			saveImage(viewer, eType);
			break;
		default:
			exportSpectrumAsk(viewer, eType);
			jsvp.getFocusNow(true);
		}
	}

	/* (non-Javadoc)
	 * @see jspecview.export.ExportInterface#printPDF(jspecview.common.JSViewer, java.lang.String)
	 */
	public String printPDF(JSViewer viewer, String pdfFileName) {		
		if (!viewer.si.isSigned())
			return "Error: Applet must be signed for the PRINT command.";
		boolean isJob = (pdfFileName == null || pdfFileName.length() == 0);
		boolean isBase64 = (!isJob && pdfFileName.toLowerCase().startsWith("base64"));
		JSVPanel jsvp = viewer.selectedPanel;
		if (jsvp == null)
			return null;
		// this has been disabled:
    jsvp.getPanelData().closeAllDialogsExcept(AType.NONE);
		PrintLayout pl = viewer.getDialogPrint(isJob);
		if (pl == null)
			return null;
		if (isJob && pl.asPDF) {
			isJob = false;
			pdfFileName = "PDF";
		}		
		if (!isBase64 && !isJob) {
			JSVFileHelper helper = viewer.fileHelper;
			helper.setFileChooser(ExportType.PDF);
			if (pdfFileName.equals("?") || pdfFileName.equalsIgnoreCase("PDF"))
  			pdfFileName = getSuggestedFileName(viewer, ExportType.PDF);
			File file = helper.getFile(pdfFileName, jsvp, true);
			if (file == null)
				return null;
			viewer.setProperty("directoryLastExporteFile", helper.setDirLastExported(file.getParent()));
			pdfFileName = file.getAbsolutePath();
		}
		String s = null;
		try {
			OutputStream os = (isJob ? null : isBase64 ? new ByteArrayOutputStream() 
			    : new FileOutputStream(pdfFileName));
			String printJobTitle = jsvp.getPanelData().getPrintJobTitle(true);
			if (pl.showTitle) {
				printJobTitle = jsvp.getInput("Title?", "Title for Printing", printJobTitle);
				if (printJobTitle == null)
					return null;
			}
			jsvp.printPanel(pl, os, printJobTitle);
			s = (isBase64 ? Base64.getBase64(
					((ByteArrayOutputStream) os).toByteArray()).toString() : "OK");
		} catch (Exception e) {
			jsvp.showMessage(e.getMessage(), "File Error");
		}
		return s;
	}
	
  /**
   * 
   * @param jsvp
   * @param imode
   * @param index
   * @param path
   * @return  status line message
   */
  private String exportSpectrumOrImage(JSVPanel jsvp, ExportType imode,
                                              int index, String path) {
    JDXSpectrum spec;
    PanelData pd = jsvp.getPanelData();
    
    if (index < 0 && (index = pd.getCurrentSpectrumIndex()) < 0)
      return "Error exporting spectrum: No spectrum selected";
    spec = pd.getSpectrumAt(index);
    int startIndex = pd.getStartingPointIndex(index);
    int endIndex = pd.getEndingPointIndex(index);
    String msg;
    try {
      switch (imode) {
      case SVG:
      case SVGI:
        msg = (new SVGExporter()).exportAsSVG(path, spec.getXYCoords(), 
            spec.getTitle(), startIndex, endIndex, spec.getXUnits(), 
            spec.getYUnits(), spec.isContinuous(), spec.isXIncreasing(), spec.isInverted(), 
            pd.getColor(ScriptToken.PLOTAREACOLOR), 
            pd.bgcolor, 
            pd.getCurrentPlotColor(0),
            pd.getColor(ScriptToken.GRIDCOLOR),
            pd.getColor(ScriptToken.TITLECOLOR),
            pd.getColor(ScriptToken.SCALECOLOR),
            pd.getColor(ScriptToken.UNITSCOLOR),
            imode == ExportType.SVGI);
        break;
      default:
        msg = exportTheSpectrum(imode.name(), path, spec, startIndex, endIndex);
      }
      return "OK - Exported " + imode.name() + ": " + path + msg;
    } catch (IOException ioe) {
      return "Error exporting " + path + ": " + ioe.getMessage();
    }
  }
  
	private void saveImage(JSViewer viewer, ExportType itype) {
  	JSVPanel jsvp = viewer.selectedPanel;
		viewer.fileHelper.setFileChooser(itype);
		String name = getSuggestedFileName(viewer, itype);
		Object file = viewer.fileHelper.getFile(name, jsvp, true);
		if (file != null)
			jsvp.saveImage(itype.toString().toLowerCase(), file);
	}

	/**
	 * 
	 * @param viewer 
	 * @param eType
	 * @return directory saved to or a message starting with "Error:"
	 */
	private String exportSpectrumAsk(JSViewer viewer, ExportType eType) {
		viewer.fileHelper.setFileChooser(eType);
		String[] items = getExportableItems(viewer, eType.equals(ExportType.SOURCE));
		JSVPanel jsvp = viewer.selectedPanel;
		int index = (items == null ? -1 : viewer.getOptionFromDialog(items, "Export", "Choose a spectrum to export"));
		if (index == Integer.MIN_VALUE)
			return null;
		File file = viewer.fileHelper.getFile(getSuggestedFileName(viewer, eType), jsvp, true);
		if (file == null)
			return null;
    String msg = "OK";
    if (eType == ExportType.SOURCE)
      JSVFileManager.fileCopy(jsvp.getPanelData().getSpectrum().getFilePath(), file);
    else
      msg = exportSpectrumOrImage(jsvp, eType, index, file
          .getAbsolutePath());
    boolean isOK = msg.startsWith("OK");
    if (isOK)
    	viewer.si.siUpdateRecentMenus(file.getAbsolutePath());
    return msg;
  }
  
	private String[] getExportableItems(JSViewer viewer,
			boolean isSameType) {
		JSVPanel jsvp = viewer.selectedPanel;
		boolean isView = viewer.currentSource.isView;
		// From popup menu click SaveAs or Export
		// if JSVPanel has more than one spectrum...Choose which one to export
		int nSpectra = jsvp.getPanelData().getNumberOfSpectraInCurrentSet();
		if (nSpectra == 1 || !isView && isSameType
				|| jsvp.getPanelData().getCurrentSpectrumIndex() >= 0)
			return null;
		String[] items = new String[nSpectra];
		for (int i = 0; i < nSpectra; i++)
			items[i] = jsvp.getPanelData().getSpectrumAt(i).getTitle();
		return items;
	}

	private String getSuggestedFileName(JSViewer viewer, ExportType imode) {
		PanelData pd = viewer.selectedPanel.getPanelData();
    String sourcePath = pd.getSpectrum().getFilePath();
    String newName = JSVFileManager.getName(sourcePath);
    int pt = newName.lastIndexOf(".");
    String name = (pt < 0 ? newName : newName.substring(0, pt));
    String ext = ".jdx";
    boolean isPrint = false;
    switch (imode) {
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
    case SOURCE:
    	if (!(name.endsWith("_" + imode)))
    		name += "_" + imode;    		
      ext = ".jdx";
      break;
    case AML:
    	ext = ".xml";
    	break;
    case JPG:
    case PNG:
    case PDF:
    	isPrint = true;
			//$FALL-THROUGH$
		default:
      ext = "." + imode.toString().toLowerCase();
    }
    if (viewer.currentSource.isView)
    	name = pd.getPrintJobTitle(isPrint);
    name += ext;
    return name;
	}


}

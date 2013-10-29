package jspecview.export;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;

import javajs.util.List;

import org.jmol.api.Interface;
import org.jmol.io.Base64;
import org.jmol.io.JmolOutputChannel;
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
import jspecview.common.Annotation.AType;
import jspecview.util.JSVTxt;

public class Exporter implements ExportInterface {

	public Exporter() {
		// for reflection; called directly only from MainFrame
	}

	public String write(JSViewer viewer, List<String> tokens, boolean forInkscape) {
		// MainFrame or applet WRITE command
		String type = null;
		String fileName = null;
		JSVPanel jsvp = viewer.selectedPanel;
		ExportType eType;
		if (tokens == null)
			return printPDF(viewer, null);
		try {
			switch (tokens.size()) {
			default:
				return "WRITE what?";
			case 1:
				fileName = Txt.trimQuotes(tokens.get(0));
				if (fileName.indexOf(".") >= 0) {
					type = "XY";
					break;
				}
				if (jsvp == null)
					return null;
				eType = ExportType.getType(fileName);
				switch (eType) {
				case PDF:
				case PNG:
				case JPG:
					return exportTheSpectrum(viewer, eType, null, null, -1, -1, null);
				default:
					// select a spectrum
					viewer.fileHelper.setFileChooser(eType);
					String[] items = getExportableItems(viewer, eType.equals(ExportType.SOURCE));
					int index = (items == null ? -1 : viewer.getOptionFromDialog(items, "Export", "Choose a spectrum to export"));
					if (index == Integer.MIN_VALUE)
						return null;
					File file = viewer.fileHelper.getFile(getSuggestedFileName(viewer, eType), jsvp, true);
					if (file == null)
						return null;
			    String msg = exportSpectrumOrImage(viewer, eType, index, file
			          .getAbsolutePath());
			    boolean isOK = msg.startsWith("OK");
			    if (isOK)
			    	viewer.si.siUpdateRecentMenus(file.getAbsolutePath());
			    return msg;
				}
			case 2:
				type = tokens.get(0).toUpperCase();
				fileName = Txt.trimQuotes(tokens.get(1));
				break;
			}
			String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
					.toUpperCase();
			if (ext.equals("JDX")) {
				if (type == null)
					type = "XY";
			} else if (ExportType.isExportMode(ext)) {
				type = ext;
			} else if (ExportType.isExportMode(type)) {
				fileName += "." + type;
			}
			eType = ExportType.getType(type);
			if (forInkscape && eType == ExportType.SVG)
				eType = ExportType.SVGI;
			return exportSpectrumOrImage(viewer, eType, -1, fileName);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

  /**
   * 
   * This export method will clip the data based on the current display
   * 
   * @param viewer 
   * @param eType
   * @param index
   * @param path
   * @return  status line message
   */
  private String exportSpectrumOrImage(JSViewer viewer, ExportType eType,
                                              int index, String path) {
    JDXSpectrum spec;
    PanelData pd = viewer.selectedPanel.getPanelData();    
    if (index < 0 && (index = pd.getCurrentSpectrumIndex()) < 0)
      return "Error exporting spectrum: No spectrum selected";
    spec = pd.getSpectrumAt(index);
    int startIndex = pd.getStartingPointIndex(index);
    int endIndex = pd.getEndingPointIndex(index);
    try {
    	String  msg = exportTheSpectrum(viewer, eType, path, spec, startIndex, endIndex, pd);
      return "OK - Exported " + eType.name() + ": " + path + msg;
    } catch (Exception ioe) {
      return "Error exporting " + path + ": " + ioe;
    }
  }
  
	public String exportTheSpectrum(JSViewer viewer, ExportType mode, String path,
			JDXSpectrum spec, int startIndex, int endIndex, PanelData pd) throws Exception {
  	JSVPanel jsvp = viewer.selectedPanel;
  	String type = mode.name();
		switch (mode) {
		case AML:
		case CML:
		case SVG:
		case SVGI:
			break;
		case DIF:
		case DIFDUP:
		case FIX:
		case PAC:
		case SQZ:
		case XY:
			type = "JDX";
			break;
		case JPG:
		case PNG:
	  	if (jsvp == null)
	  		return null;
			viewer.fileHelper.setFileChooser(mode);
			String name = getSuggestedFileName(viewer, mode);
			Object file = viewer.fileHelper.getFile(name, jsvp, true);
			return (file == null ? null : jsvp.saveImage(type.toLowerCase(), file));
		case PDF:
			return printPDF(viewer, "PDF");
		case SOURCE:
	  	if (jsvp == null)
	  		return null;
	    return fileCopy(jsvp.getPanelData().getSpectrum().getFilePath(), path);
		case UNK:
			return null;
		}
		return ((ExportInterface) Interface.getInterface("jspecview.export." + type.toUpperCase() + "Exporter")).exportTheSpectrum(viewer, mode, path, spec, startIndex, endIndex, null);
	}

	private String printPDF(JSViewer viewer, String pdfFileName) {
		if (!viewer.si.isSigned())
			return "Error: Applet must be signed for the PRINT command.";
		boolean isJob = (pdfFileName == null || pdfFileName.length() == 0);
		boolean isBase64 = (!isJob && pdfFileName.toLowerCase()
				.startsWith("base64"));
		JSVPanel jsvp = viewer.selectedPanel;
		if (jsvp == null)
			return null;
		jsvp.getPanelData().closeAllDialogsExcept(AType.NONE);
		PrintLayout pl;
		/**
		 * @j2sNative
		 * 
		 *    pl = new jspecview.common.PrintLayout();
		 *    pl.asPDF = true;
		 */
		{
			pl = viewer.getDialogPrint(isJob);
	    if (pl == null)
	      return null;
		}
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
			viewer.setProperty("directoryLastExporteFile", helper
					.setDirLastExported(file.getParent()));
			pdfFileName = file.getAbsolutePath();
		}
		String s = null;
		try {
			OutputStream os = null;
			/**
			 * @j2sNative
			 * 
			 * 
			 */
			{
				os = (isBase64 ? null : new FileOutputStream(pdfFileName, false));
			}
			JmolOutputChannel out = (isJob ? null : new JmolOutputChannel()
					.setParams(viewer, isBase64 ? null : pdfFileName, false, os));
			String printJobTitle = jsvp.getPanelData().getPrintJobTitle(true);
			if (pl.showTitle) {
				printJobTitle = jsvp.getInput("Title?", "Title for Printing",
						printJobTitle);
				if (printJobTitle == null)
					return null;
			}
			jsvp.printPanel(pl, out, printJobTitle);
			s = (isBase64 ? Base64.getBase64(out.toByteArray()).toString() : out.toString());
		} catch (Exception e) {
			jsvp.showMessage(e.getMessage(), "File Error");
		}
		return s;
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

  private static String fileCopy(String name, String path) {
    try {
      BufferedReader br = JSVFileManager.getBufferedReaderFromName(name,
          null);
      FileWriter writer = new FileWriter(path);
      String line = null;
      while ((line = br.readLine()) != null) {
        writer.write(line);
        writer.write(JSVTxt.newLine);
      }
      writer.close();
      return "OK";
    } catch (Exception e) {
      return e.toString();
    }
  }


}

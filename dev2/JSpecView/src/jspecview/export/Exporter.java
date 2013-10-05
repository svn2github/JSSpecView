package jspecview.export;

import java.io.File;
import java.io.IOException;

import org.jmol.util.JmolList;
import org.jmol.util.Txt;

import jspecview.api.ScriptInterface;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanel;
import jspecview.common.PanelData;
import jspecview.common.ScriptToken;
import jspecview.util.JSVFileManager;

public class Exporter {

  public final static String sourceLabel = "Original...";
  public enum ExportType {
    UNK, SOURCE, DIF, FIX, SQZ, PAC, XY, DIFDUP, PNG, JPG, SVG, SVGI, CML, AML, PDF;

    public static ExportType getType(String type) {
      type = type.toUpperCase();
      if (type.equalsIgnoreCase(sourceLabel))
        return SOURCE;
      if (type.startsWith("XML"))
        return AML;
      for (ExportType mode : values())
        if (mode.name().equals(type)) 
          return mode;
      return UNK;
    }    
    
    public static boolean isExportMode(String ext) {
      return (getType(ext) != UNK);
    }
  }
  
  /**
   * Auxiliary Export method
   * @param si 
   * @param type
   *        the format to export in
   * @param index
   *        the index of the spectrum
   * @param file
   * @return message or null if canceled
   */
	public static String exportSpectrum(ScriptInterface si,
                                              ExportType type, int index,
                                              File file) {
		if (file == null)
			return null;
		JSVPanel jsvp = si.getSelectedPanel();
    String msg = "OK";
    if (type == ExportType.SOURCE)
      JSVFileManager.fileCopy(jsvp.getPanelData().getSpectrum().getFilePath(), file);
    else
      msg = exportSpectrumOrImage(jsvp, type, index, file
          .getAbsolutePath());
    boolean isOK = msg.startsWith("OK");
    if (isOK)
    	si.updateRecentMenus(file.getAbsolutePath());
    return msg;
  }
  
	public static String getSuggestedFileName(ScriptInterface si, ExportType imode) {
		PanelData pd = si.getSelectedPanel().getPanelData();
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
    if (si.getCurrentSource().isView)
    	name = pd.getPrintJobTitle(isPrint);
    name += ext;
    return name;
	}

  /**
   * from EXPORT command
   * @param jsvp 
   * @param tokens
   * @param forInkscape 
   * 
   * @return message for status line
   */
  public static String exportCmd(JSVPanel jsvp, JmolList<String> tokens, boolean forInkscape) {
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


  /**
   * 
   * @param jsvp
   * @param imode
   * @param index
   * @param path
   * @return  status line message
   */
  private static String exportSpectrumOrImage(JSVPanel jsvp, ExportType imode,
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
            jsvp.getBackgroundColor(), 
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
  
  /**
   * returns message if path is not null, otherwise full string of text (unsigned applet)
   * @param type 
   * @param path
   * @param spec
   * @param startIndex
   * @param endIndex
   * @return message or text
   * @throws IOException
   */
  public static String exportTheSpectrum(String type, String path, 
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

	public static String[] getExportableItems(ScriptInterface si,
			boolean isSameType) {
		JSVPanel jsvp = si.getSelectedPanel();
		boolean isView = si.getCurrentSource().isView;
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



}

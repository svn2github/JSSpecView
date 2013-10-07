package jspecview.java;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import jspecview.api.JSVFileHelper;
import jspecview.common.ExportType;
import jspecview.common.JSViewer;

public class AwtFileHelper implements JSVFileHelper {
	
	public String dirLastOpened;
	public boolean useDirLastOpened;
	public boolean useDirLastExported;
	public String dirLastExported;

	private JFileChooser fc;
	private JSViewer viewer;

	public AwtFileHelper(JSViewer viewer) {
		this.viewer = viewer;
	}
	
	public void setFileChooser(ExportType imode) {
		if (fc == null)
		  fc = new JFileChooser();
    AwtDialogFileFilter filter = new AwtDialogFileFilter();
    fc.resetChoosableFileFilters();
    switch (imode) {
    case UNK:
  		filter = new AwtDialogFileFilter();
  		filter.addExtension("xml");
  		filter.addExtension("aml");
  		filter.addExtension("cml");
  		filter.setDescription("CML/XML Files");
  		fc.setFileFilter(filter);
  		filter = new AwtDialogFileFilter();
  		filter.addExtension("jdx");
  		filter.addExtension("dx");
  		filter.setDescription("JCAMP-DX Files");
  		fc.setFileFilter(filter);
    	break;
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
    case SOURCE:
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      break;
    default:
      filter.addExtension(imode.toString().toLowerCase());
      filter.setDescription(imode + " Files");
    }
    fc.setFileFilter(filter);    
	}

	public File showFileOpenDialog(Frame frame) {
		setFileChooser(ExportType.UNK);
		return getFile("", frame, false);
	}

	public File getFile(String name, Object panelOrFrame, boolean isSave) {
		Component c = (Component) panelOrFrame;
		fc.setSelectedFile(new File(name));
		if (isSave) {
			if (useDirLastExported)
				fc.setCurrentDirectory(new File(dirLastExported));
		} else {
			if (useDirLastOpened)
				fc.setCurrentDirectory(new File(dirLastOpened));
		}
		int returnVal = (isSave ? fc.showSaveDialog(c) : fc.showOpenDialog(c));
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return null;
		File file = fc.getSelectedFile();
		if (isSave) {
			viewer.setProperty("directoryLastExportedFile", dirLastExported = file.getParent());
	    if (file.exists()) {
	      int option = JOptionPane.showConfirmDialog(c,
	          "Overwrite " + file.getName() + "?", "Confirm Overwrite Existing File",
	          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	      if (option == JOptionPane.NO_OPTION)
	        return null;
	    }
		} else {
			viewer.setProperty("directoryLastOpenedFile", dirLastOpened = file.getParent());
		}
		return file;
	}

	public String setDirLastExported(String name) {
		return dirLastExported = name;
	}


}

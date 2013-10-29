package jspecview.api;

import org.jmol.api.JmolFileInterface;

import jspecview.common.ExportType;
import jspecview.common.JSViewer;

public interface JSVFileHelper {

	void setFileChooser(ExportType pdf);

	JmolFileInterface getFile(String fileName, Object panelOrFrame, boolean b);

	String setDirLastExported(String name);

	JSVFileHelper set(JSViewer jsViewer);

}

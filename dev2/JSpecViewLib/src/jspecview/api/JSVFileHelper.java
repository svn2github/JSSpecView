package jspecview.api;

import javajs.api.GenericFileInterface;

import jspecview.common.ExportType;
import jspecview.common.JSViewer;

public interface JSVFileHelper {

	void setFileChooser(ExportType pdf);

	GenericFileInterface getFile(String fileName, Object panelOrFrame, boolean b);

	String setDirLastExported(String name);

	JSVFileHelper set(JSViewer jsViewer);

}

package jspecview.api;

import java.io.File;

import jspecview.common.ExportType;

public interface JSVFileHelper {

	void setFileChooser(ExportType pdf);

	File getFile(String fileName, Object panelOrFrame, boolean b);

	String setDirLastExported(String name);

}

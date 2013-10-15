package jspecview.awtjs2d;

import java.io.File;

import jspecview.api.JSVFileHelper;
import jspecview.common.ExportType;
import jspecview.common.JSViewer;

public class JsFileHelper implements JSVFileHelper {

	private JSViewer viewer;

	public JsFileHelper() {
	}

	public JSVFileHelper set(JSViewer viewer) {
		this.viewer = viewer;
		return this;
	}

	public File getFile(String fileName, Object panelOrFrame, boolean b) {
		// TODO Auto-generated method stub
		return null;
	}

	public String setDirLastExported(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setFileChooser(ExportType pdf) {
		// TODO Auto-generated method stub

	}

}

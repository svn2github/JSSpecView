package jspecview.js2d;

import javajs.api.GenericFileInterface;

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

	@SuppressWarnings("null")
	public GenericFileInterface getFile(String fileName, Object panelOrFrame, boolean isSave) {
		String f = null;
		/**
		 * @j2sNative
		 * 
		 * f = prompt("Enter a file name:", fileName);
		 * 
		 */
		{
		}
		return (f == null ? null : new JsFile(f));
	}

	public String setDirLastExported(String name) {
		return name;
	}

	public void setFileChooser(ExportType pdf) {
		// TODO Auto-generated method stub

	}

}

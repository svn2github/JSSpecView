package jspecview.js2d;

import javajs.api.GenericFileInterface;

import jspecview.api.JSVFileHelper;
import jspecview.common.ExportType;
import jspecview.common.JSVFileManager;
import jspecview.common.JSViewer;

public class JsFileHelper implements JSVFileHelper {

	private JSViewer viewer;

	public JsFileHelper() {
	}

	@Override
	public JSVFileHelper set(JSViewer viewer) {
		this.viewer = viewer;
		return this;
	}

	@Override
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

	@Override
	public String setDirLastExported(String name) {
		return name;
	}

	@Override
	public void setFileChooser(ExportType pdf) {
		// TODO Auto-generated method stub

	}

	@Override
	public GenericFileInterface showFileOpenDialog(Object panelOrFrame, boolean isAppend) {
		
		Object applet = viewer.applet;
		/**
		 * @j2sNative
		 * 
		 * 	Jmol._loadFileAsynchronously(this, applet, "", isAppend);
		 * 
		 */
		{
			System.out.println(applet);
		}
		return null;
	}

  /**
   * Called by Jmol._loadFileAsyncDone(this.viewer.applet). Allows for callback
   * to set the file name.
   * 
   * @param fileName
   * @param data
   * @param isAppend
   * @throws InterruptedException
   */
  void setData(String fileName, Object data, Object isAppend) throws InterruptedException {
    if (fileName == null)
    	return;
    if (data == null) {
    	viewer.selectedPanel.showMessage(fileName, "File Open Error");
    	return;
    }
    // this file name
    viewer.si.siOpenDataOrFile(new String((byte[]) data), fileName, null, null, -1, -1, isAppend != null);
  }   

  @Override
	public String getUrlFromDialog(String info, String msg) {
		/**
		 * @j2sNative
		 * 
		 * return prompt(info, msg);
		 * 
		 */
		{
			return null;
  	}
	}

}

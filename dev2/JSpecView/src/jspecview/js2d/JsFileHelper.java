package jspecview.js2d;

import javajs.api.GenericFileInterface;
import javajs.util.PT;

import jspecview.api.JSVFileHelper;
import jspecview.common.ExportType;
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
		fileName = PT.rep(fileName,  "=",  "_");
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
	public GenericFileInterface showFileOpenDialog(Object panelOrFrame, Object[] userData) {
	  // userData[0]: isAppend
	  // userData[1]: script
		
		Object applet = viewer.applet;
		/**
		 * @j2sNative
		 * 
		 * 	Jmol._loadFileAsynchronously(this, applet, "", userData);
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
   * @param userInfo   [0] isAppend; [1] script 
   * @throws InterruptedException
   */
  void setData(String fileName, Object data, Object[] userInfo) throws InterruptedException {
    if (fileName == null)
    	return;
    if (data == null) {
    	viewer.selectedPanel.showMessage(fileName, "File Open Error");
    	return;
    }
    String script = (userInfo == null ? null : "");
    boolean isAppend = false;
    /**
     * @j2sNative
     * 
     * isAppend = userInfo[0];
     * script = userInfo[1];
     */
    {
    }
    // this file name
    viewer.si.siOpenDataOrFile(new String((byte[]) data), fileName, null, null, -1, -1, isAppend, null);
    if (script != null)
      viewer.runScript(script);
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

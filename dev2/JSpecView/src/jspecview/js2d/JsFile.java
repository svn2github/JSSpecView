package jspecview.js2d;

import java.net.URL;

import jspecview.common.JSVFileManager;
import javajs.api.GenericFileInterface;
import javajs.util.AjaxURLConnection;
import javajs.util.PT;


/**
 * 
 * A class that mimics java.io.File
 * 
 */

class JsFile implements GenericFileInterface {

  private String name;
	private String fullName;

  static GenericFileInterface newFile(String name) {
    return new JsFile(name);
  }

	JsFile(String name) {
  	this.name = name.replace('\\','/');
  	fullName = name;
  	if (!fullName.startsWith("/") && JSVFileManager.urlTypeIndex(name) < 0)
  		fullName = JSVFileManager.jsDocumentBase + "/" + fullName;
  	fullName = PT.rep(fullName, "/./", "/");
  	name = name.substring(name.lastIndexOf("/") + 1);
  }

  public GenericFileInterface getParentAsFile() {
  	int pt = fullName.lastIndexOf("/");
  	return (pt < 0 ? null : new JsFile(fullName.substring(0, pt)));
  }

	public String getFullPath() {
		return fullName;
	}

	public String getName() {
    return name;
	}

	public boolean isDirectory() {
		return fullName.endsWith("/");
	}

	public long length() {
		return 0; // can't do this, shouldn't be necessary
	}

  static Object getBufferedURLInputStream(URL url, byte[] outputBytes,
      String post) {
    try {
      AjaxURLConnection conn = (AjaxURLConnection) url.openConnection();
      if (outputBytes != null)
        conn.outputBytes(outputBytes);
      else if (post != null)
        conn.outputString(post);
      return conn.getSB();
    } catch (Exception e) {
      return e.toString();
    }
  }

}

package jspecview.api;

import java.net.URL;

import javajs.util.List;


import jspecview.app.JSVApp;
import jspecview.common.JSViewer;
import jspecview.source.JDXSpectrum;

public interface AppletFrame {

	URL getDocumentBase();

	void setDropTargetListener(boolean isSigned, JSViewer viewer);

	String getAppletInfo();

	void addNewPanel(JSViewer viewer);

	void newWindow(boolean isSelected);

	void repaint();

	void validate();

	String getParameter(String name);

	void callToJavaScript(String callbackName, Object[] data);

	void setPanelVisible(boolean b);

	void validateContent(int mode);

	JSVPanel getJSVPanel(JSViewer viewer, List<JDXSpectrum> specs,
			int initialStartIndex, int initialEndIndex);

	void doExitJmol();

	JSVApp getApp();

}

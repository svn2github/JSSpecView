package jspecview.api;

import java.net.URL;

import org.jmol.util.JmolList;

import jspecview.app.JSVApp;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSViewer;
import jspecview.common.PrintLayout;

public interface AppletFrame {

	URL getDocumentBase();

	void setPlatformFields(boolean isSigned, JSViewer viewer);

	String getAppletInfo();

	void addNewPanel(JSViewer viewer);

	void newWindow(boolean isSelected);

	void repaint();

	void validate();

	String getParameter(String name);

	void callToJavaScript(String callbackName, Object[] data);

	void setPanelVisible(boolean b);

	PrintLayout getDialogPrint(boolean isJob);

	void validateContent(int mode);

	JSVPanel getJSVPanel(JSViewer viewer, JmolList<JDXSpectrum> specs,
			int initialStartIndex, int initialEndIndex);

	JSVDialog newDialog(JSViewer viewer, String type);

	void showWhat(JSViewer viewer, String what);

	JSVApiPlatform getApiPlatform();

	JSVGraphics getG2D();

	void doExitJmol();

	JSVApp getApp();

}

package jspecview.api;

import org.jmol.util.JmolList;

public interface JSVAppInterface extends JSVAppletInterface, ScriptInterface {

	void newWindow(boolean isSelected, boolean fromFrame);

	JmolList<String> getScriptQueue();

}

package jspecview.api;

import javajs.util.List;

public interface JSVAppInterface extends JSVAppletInterface, ScriptInterface {

	void siNewWindow(boolean isSelected, boolean fromFrame);

	List<String> getScriptQueue();

}

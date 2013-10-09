package jspecview.api;

import org.jmol.api.ApiPlatform;
import org.jmol.api.JmolMouseInterface;

public interface JSVApiPlatform extends ApiPlatform {
	JSVPopupMenu getJSVMenuPopup(String menu);
	JmolMouseInterface getMouseManager(JSVPanel jsvp);
}

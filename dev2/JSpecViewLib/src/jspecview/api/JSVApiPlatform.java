package jspecview.api;

import org.jmol.api.ApiPlatform;
import org.jmol.api.JmolMouseInterface;

public interface JSVApiPlatform extends ApiPlatform {
	JmolMouseInterface getMouseManager(JSVPanel jsvp);
}

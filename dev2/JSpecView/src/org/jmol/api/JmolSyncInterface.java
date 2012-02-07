package org.jmol.api;


/*
 * This interface can be used by any applet to register itself
 * with Jmol and thus allow direct applet-applet scripting and 
 * syncing operations.
 *  
 */
public interface JmolSyncInterface {

  public abstract void syncScript(String script);

  public abstract void registerApplet(String appletID, JmolSyncInterface applet);

}

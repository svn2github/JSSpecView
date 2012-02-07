package org.jmol.api;


/*
 * Used for application - application interface (Jmol-JSpecView)
 *  
 */
public interface JmolSyncInterface {

  public abstract void syncScript(String script);

  public abstract void registerApplet(String appletID, JmolSyncInterface jsi);

}

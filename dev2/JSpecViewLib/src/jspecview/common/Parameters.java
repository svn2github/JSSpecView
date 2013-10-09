package jspecview.common;

import java.util.Hashtable;
import java.util.Map;

import jspecview.api.JSVPanel;

public class Parameters {

	public String name;
  public double integralMinY = IntegralData.DEFAULT_MINY;
  public double integralRange = IntegralData.DEFAULT_RANGE;
  public double integralOffset = IntegralData.DEFAULT_OFFSET;
	public boolean integralDrawAll = false;

  public double peakListThreshold = Double.NaN; // <= 0 disables these
  public String peakListInterpolation = "parabolic";
  public int precision = 2;

	public Parameters(String name) {
    this.name = name;
    setBoolean(ScriptToken.TITLEON, true);
    setBoolean(ScriptToken.ENABLEZOOM, true);
    setBoolean(ScriptToken.DISPLAY2D, true);
    setBoolean(ScriptToken.COORDINATESON, true);
    setBoolean(ScriptToken.GRIDON, true);
    setBoolean(ScriptToken.XSCALEON, true);
    setBoolean(ScriptToken.YSCALEON, true);
    setBoolean(ScriptToken.XUNITSON, true);
    setBoolean(ScriptToken.YUNITSON, true);
	}

  protected Map<ScriptToken, Boolean> htBooleans = new Hashtable<ScriptToken, Boolean>();
	
  public Map<ScriptToken, Boolean> getBooleans() {
    return htBooleans;
  }

  public boolean setBoolean(ScriptToken st, boolean val) {
    htBooleans.put(st, Boolean.valueOf(val));
    return val;
  }

  public boolean getBoolean(ScriptToken t) {
    return Boolean.TRUE == htBooleans.get(t);
  }
    
  public static boolean isTrue(String value) {
    return (value.length() == 0 || Boolean.parseBoolean(value)); 
  }
  
	public static Boolean getTFToggle(String value) {
		return (value.equalsIgnoreCase("TOGGLE") ? null
				: isTrue(value) ? Boolean.TRUE : Boolean.FALSE);
	}

	public void setP(JSVPanel jsvp, ScriptToken st, String value) {
		switch (st) {
		default:
			return;
		case COORDINATESON:
		case DISPLAY1D:
		case DISPLAY2D:
		case ENABLEZOOM:
		case GRIDON:
		case REVERSEPLOT:
		case TITLEON:
		case TITLEBOLDON:
		case XSCALEON:
		case XUNITSON:
		case YSCALEON:
		case YUNITSON:
			Boolean tfToggle = getTFToggle(value);
			if (tfToggle != null) {
				setBoolean(st, tfToggle.booleanValue());
				break;
			}
			if (jsvp == null)
				return;
			boolean b = !jsvp.getPanelData().getBoolean(st);
			switch (st) {
			default:
				break;
			case XSCALEON:
				setBoolean(ScriptToken.XUNITSON, b);
				jsvp.getPanelData().setBoolean(ScriptToken.XUNITSON, b);
				break;
			case YSCALEON:
				setBoolean(ScriptToken.YUNITSON, b);
				jsvp.getPanelData().setBoolean(ScriptToken.YUNITSON, b);
				break;
			}
			setBoolean(st, b);
			break;
		}
		if (jsvp == null)
			return;
		jsvp.getPanelData().setBoolean(this, st);
	}
	
	public static boolean isMatch(String match, String key) {
		return match == null || key.equalsIgnoreCase(match);
	}

	public static void putInfo(String match, Map<String, Object> info,
	                           String key, Object value) {
	  if (value != null && isMatch(match, key))
	    info.put(match == null ? key : match, value);
	}



}

package jspecview.common;

import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import jspecview.common.Graph;
import jspecview.common.IntegralGraph;
import jspecview.common.IntegrationRatio;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;

public class AppUtils {
	
	// defined in ManFrame.java and JSVApplet.java
	  public static Color integralPlotColor = Color.red;
	
	/*------------------------- Integration functions --------------------------*/

	/**
	     * Integrates an HNMR spectrum
	     * @param owner parent container for JInternalFrame frame
	     * @param frame the selected frame
	     * @param showDialog if true then dialog is shown, otherwise spectrum is
	     * integrated with default values
	     * @return the panel containing the HNMR spectrum with integral displayed
	     */
	    public static JSVPanel integrate(Frame owner, JInternalFrame frame, boolean showDialog){
	      if(frame == null)
	        return null;
	
	      JSVPanel jsvp = JSVPanel.getPanel0(frame);
	      boolean integrateOn = false;
	      int numGraphs = jsvp.getNumberOfSpectra();
	      boolean allowIntegration = false;
	      IntegralGraph integGraph = null;
	      IntegrateDialog integDialog;
	      JDXSpectrum spectrum = jsvp.getSpectrumAt(0);
	
	      if (numGraphs == 1){
	        allowIntegration = spectrum.isHNMR();
	      }
	      else if((integrateOn = AppUtils.hasIntegration(jsvp)) == true){
	        allowIntegration = integrateOn;
	      }
	
	      if(allowIntegration){
	        if(showDialog){
	          if(integrateOn){
	            IntegralGraph graph = (IntegralGraph)jsvp.getIntegralGraph();
	            integDialog = new IntegrateDialog(owner, "Integration Parameters", true,
	                graph.getPercentMinimumY(),
	                graph.getPercentOffset(),
	                graph.getIntegralFactor());
	          } else{
	            integDialog = new IntegrateDialog(owner, "Integration Parameters", true,
	                Double.parseDouble(JSpecViewUtils.integralMinY), Double.parseDouble(JSpecViewUtils.integralOffset),
	                Double.parseDouble(JSpecViewUtils.integralFactor));
	          }
	
	          integGraph = new IntegralGraph(spectrum, integDialog.getMinimumY(),
	                                         integDialog.getOffset(), integDialog.getFactor());
	        } else{
	          integGraph = new IntegralGraph(spectrum, Double.parseDouble(JSpecViewUtils.integralMinY),
	                                         Double.parseDouble(JSpecViewUtils.integralOffset),
	                                         Double.parseDouble(JSpecViewUtils.integralFactor));
	        }
	        integGraph.setXUnits(spectrum.getXUnits());
	        integGraph.setYUnits(spectrum.getYUnits());
	
	        try {
	          if(integGraph != null){
	            JSVPanel newJsvp = new JSVPanel(new Graph[]{spectrum, integGraph});
	            newJsvp.setTitle(integGraph.getTitle());
	            //newJsvp.setOverlayIncreasing(spectrum.isIncreasing());
	            //newJsvp.addMouseListener(new JSVPanelPopupListener(jsvpPopupMenu, newJsvp, currentSelectedSource));
	
	            // zoom turned off after integration?
	            newJsvp.setZoomEnabled(false);
	            frame.getContentPane().remove(jsvp);
	            frame.getContentPane().add(newJsvp);
	            newJsvp.setPlotColors(new Color[]{newJsvp.getPlotColor(0), integralPlotColor});
	            jsvp.setEnabled(true);
	            newJsvp.setEnabled(true);
	          }
	        }
	        catch (ScalesIncompatibleException ex) {
	        }
	      }
	      return JSVPanel.getPanel0(frame);
	  }

	public static JSVPanel appletIntegrate(JPanel panel, boolean showDialog, ArrayList<IntegrationRatio> integrationRatios) {
	    if (panel == null)
	      return null;
	
	    JSVPanel jsvp = (JSVPanel) panel.getComponent(0);
	    boolean integrateOn = false;
	    int numGraphs = jsvp.getNumberOfSpectra();
	    boolean allowIntegration = false;
	    IntegralGraph integGraph = null;
	    IntegrateDialog integDialog;
	    JDXSpectrum spectrum = jsvp.getSpectrumAt(0);
	
	    if (numGraphs == 1) {
	      allowIntegration = spectrum.isHNMR();
	    }
	    else if ((integrateOn = AppUtils.hasIntegration(jsvp)) == true) {
	      allowIntegration = integrateOn;
	    }
	
	    if (allowIntegration) {
	      if (showDialog) {
	        if (integrateOn) {
	          IntegralGraph graph = (IntegralGraph) jsvp.getIntegralGraph();
	          integDialog = new IntegrateDialog(panel, "Integration Parameters", true,
	                                            graph.getPercentMinimumY(),
	                                            graph.getPercentOffset(),
	                                            graph.getIntegralFactor());
	        }
	        else {
	          integDialog = new IntegrateDialog(panel, "Integration Parameters", true,
	                                            Double.parseDouble(JSpecViewUtils.integralMinY),
	                                            Double.parseDouble(JSpecViewUtils.integralOffset),
	                                            Double.parseDouble(JSpecViewUtils.integralFactor));
	        }
	
	        integGraph = new IntegralGraph(spectrum, integDialog.getMinimumY(),
	                                       integDialog.getOffset(),
	                                       integDialog.getFactor());
	      }
	      else {
	
	        integGraph = new IntegralGraph(spectrum,
	                                       Double.parseDouble(JSpecViewUtils.integralMinY),
	                                       Double.parseDouble(JSpecViewUtils.integralOffset),
	                                       Double.parseDouble(JSpecViewUtils.integralFactor));
	      }
	      integGraph.setXUnits(spectrum.getXUnits());
	      integGraph.setYUnits(spectrum.getYUnits());
	
	      try {
	        if (integGraph != null) {
	          JSVPanel newJsvp = new JSVPanel(new Graph[] {spectrum, integGraph});
	          newJsvp.setTitle(integGraph.getTitle());
	          //newJsvp.setOverlayIncreasing(spectrum.isIncreasing());
	
	          newJsvp.setPlotColors(new Color[] {newJsvp.getPlotColor(0),
	        		  				jsvp.getIntegralPlotColor()});
	          
	          // zoom turned off after integration?
	          newJsvp.setZoomEnabled(false);
	          
	          // add integration ratio annotations if any exist
	          if(integrationRatios != null)
	        	  newJsvp.setIntegrationRatios(integrationRatios);
	          
	          panel.remove(jsvp);
	          panel.add(newJsvp);
	          
	          //newJsvp.setPlotColors(new Color[] {newJsvp.getPlotColor(0),
	          //                      integralPlotColor});
	        }
	      }
	      catch (ScalesIncompatibleException ex) {
	      }
	    }
	    return (JSVPanel)panel.getComponent(0);
	  }

	/**
	   * Determines whether or not a JSVPanel already had an integral graph
	   * @param jsvp the JSVPanel
	   * @return true if had integral, false otherwise
	   */
	  public static boolean hasIntegration(JSVPanel jsvp) {
	    int numGraphs = jsvp.getNumberOfSpectra();
	    if(numGraphs == 2){
	      String title = jsvp.getIntegralGraph().getTitle();
	      if(title.toLowerCase().indexOf("integral") != -1){
	        return true;
	      }
	    }
	    return false;
	  }

	public static String removeIntegration(Container pane) {
	    JSVPanel jsvp = (JSVPanel) pane.getComponent(0);
	    try {
	      JSVPanel newJsvp = new JSVPanel(jsvp.getSpectrumAt(0));
	      newJsvp.setZoomEnabled(true);
	      pane.remove(jsvp);
	      pane.add(newJsvp);
	      return null;
	    } catch (JSpecViewException ex) {
	      return ex.getMessage();
	    }
	  }

	/**
	   * Returns a <code>Color</code> from a string representation as a hex value or
	   * a delimiter separated rgb values. The following are all valid arguments:
	   * <pre>
	   * "#ffffff"
	   * "#FFFFFF"
	   * "255 255 255"
	   * "255,255,255"
	   * "255;255;255"
	   * "255-255-255"
	   * "255.255.255"
	   * </pre>
	   * @param string the color as a string
	   * @return a <code>Color</code> from a string representation
	   */
	  public static Color getColorFromString(String string){
	    int r, g, b;
	    if(string == null){
	      return null;
	    }
	    string = string.trim();
	
	    try {
	      if(string.startsWith("#")){
	        return new Color(Integer.parseInt(string.substring(1), 16));
	      }
	      StringTokenizer st = new StringTokenizer(string, ",;.- ");
	      r = Integer.parseInt(st.nextToken().trim());
	      g = Integer.parseInt(st.nextToken().trim());
	      b = Integer.parseInt(st.nextToken().trim());
	      return new Color(r, g, b);
	    }
	    catch (NumberFormatException ex) {
	      return null;
	    }
	  }

	/**
	   * Returns a hex string representation of a <code>Color</color> object
	   * @param color the Color
	   * @return a hex string representation of a <code>Color</color> object
	   */
	  public static String colorToHexString(Color color){
	    String r = Integer.toHexString(color.getRed());
	    if(r.length() == 1)
	      r = "0" + r;
	    String g = Integer.toHexString(color.getGreen());
	    if(g.length() == 1)
	      g = "0" + g;
	    String b = Integer.toHexString(color.getBlue());
	    if(b.length() == 1)
	      b = "0" + b;
	    return "#" + r + g + b;
	  }

}

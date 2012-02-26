package jspecview.common;

import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import jspecview.common.IntegralGraph;
import jspecview.common.IntegrationRatio;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSpecViewUtils;

public class AppUtils {

  // defined in ManFrame.java and JSVApplet.java
  public static Color integralPlotColor = Color.red;

  /*------------------------- Integration functions --------------------------*/

  /**
   * Integrates an HNMR spectrum
   * 
   * @param frameOrPanel
   *        the selected frame
   * @param showDialog
   *        if true then dialog is shown, otherwise spectrum is integrated with
   *        default values
   * @param integrationRatios
   * 
   * 
   * @return the panel containing the HNMR spectrum with integral displayed
   */
  public static JSVPanel integrate(Container frameOrPanel, boolean showDialog,
                                   ArrayList<IntegrationRatio> integrationRatios) {
    JPanel jp = (JPanel) (frameOrPanel instanceof JInternalFrame ? ((JInternalFrame) frameOrPanel)
        .getContentPane()
        : frameOrPanel);
    JSVPanel jsvp = (JSVPanel) jp.getComponent(0);
    JDXSpectrum spectrum = jsvp.getSpectrum();
    IntegralGraph graph = (IntegralGraph) spectrum.getIntegrationGraph();
    spectrum.setIntegrationGraph(null);
    if (graph != null || spectrum.isHNMR() && jsvp.getNumberOfSpectra() == 1) {
      if (showDialog) {
        IntegrateDialog integDialog;
        if (graph != null) {
          integDialog = new IntegrateDialog(jp, "Integration Parameters", true,
              graph.getPercentMinimumY(), graph.getPercentOffset(), graph
                  .getIntegralFactor());
        } else {
          integDialog = new IntegrateDialog(jp, "Integration Parameters", true,
              JSpecViewUtils.integralMinY, JSpecViewUtils.integralOffset,
              JSpecViewUtils.integralFactor);
        }
        graph = new IntegralGraph(spectrum, integDialog.getMinimumY(),
            integDialog.getOffset(), integDialog.getFactor());
      } else {
        graph = new IntegralGraph(spectrum, JSpecViewUtils.integralMinY,
            JSpecViewUtils.integralOffset, JSpecViewUtils.integralFactor);
      }
      graph.setXUnits(spectrum.getXUnits());
      graph.setYUnits(spectrum.getYUnits());
      spectrum.setIntegrationGraph(graph);
      JSVPanel newJsvp = JSVPanel.getIntegralPanel(spectrum);
      newJsvp.setTitle(graph.getTitle());
      newJsvp.setPlotColors(new Color[] { newJsvp.getPlotColor(0),
          jsvp.getIntegralPlotColor() });
      // add integration ratio annotations if any exist
      if (integrationRatios != null)
        newJsvp.setIntegrationRatios(integrationRatios);
      jp.remove(jsvp);
      jp.add(newJsvp);
    }
    return (JSVPanel) jp.getComponent(0);
  }

  /**
   * Determines whether or not a JSVPanel already had an integral graph
   * 
   * @param jsvp
   *        the JSVPanel
   * @return true if had integral, false otherwise
   */
  public static boolean hasIntegration(JSVPanel jsvp) {
    return (jsvp.getSpectrum().getIntegrationGraph() != null);
  }

  public static JSVPanel removeIntegration(Container pane) {
    JSVPanel jsvp = (JSVPanel) pane.getComponent(0);
    JDXSpectrum spectrum = jsvp.getSpectrum();
    spectrum.setIntegrationGraph(null);
    JSVPanel newJsvp = new JSVPanel(spectrum);
    pane.remove(jsvp);
    pane.add(newJsvp);
    return newJsvp;
  }

  /**
   * Returns a hex string representation of a <code>Color</color> object
   * 
   * @param color
   *        the Color
   * @return a hex string representation of a <code>Color</color> object
   */
  public static String colorToHexString(Color color) {
    String r = Integer.toHexString(color.getRed());
    if (r.length() == 1)
      r = "0" + r;
    String g = Integer.toHexString(color.getGreen());
    if (g.length() == 1)
      g = "0" + g;
    String b = Integer.toHexString(color.getBlue());
    if (b.length() == 1)
      b = "0" + b;
    return "#" + r + g + b;
  }

}

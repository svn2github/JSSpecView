package jspecview.common;

import java.awt.Color;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import jspecview.common.IntegralGraph;
import jspecview.common.IntegrationRatio;
import jspecview.common.JDXSpectrum;
import jspecview.util.ColorUtil;

public class AppUtils {

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
  public static JSVPanel integrate(
                                   Container frameOrPanel,
                                   boolean showDialog,
                                   ArrayList<IntegrationRatio> integrationRatios,
                                   Parameters parameters) {
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
              parameters.integralMinY, parameters.integralOffset,
              parameters.integralFactor);
        }
        parameters.integralMinY = integDialog.getMinimumY();
        parameters.integralOffset = integDialog.getOffset();
        parameters.integralFactor = integDialog.getFactor();
      }
      graph = spectrum.integrate(parameters.integralMinY,
          parameters.integralOffset, parameters.integralFactor);

      if (graph != null) {
        JSVPanel newJsvp = JSVPanel.getIntegralPanel(spectrum, jsvp
            .getIntegralPlotColor());
        // add integration ratio annotations if any exist
        if (integrationRatios != null)
          newJsvp.setIntegrationRatios(integrationRatios);
        jp.remove(jsvp);
        jp.add(newJsvp);
      }
    }
    return (JSVPanel) jp.getComponent(0);
  }

  public static JSVPanel removeIntegration(Container frameOrPanel) {
    JPanel jp = (JPanel) (frameOrPanel instanceof JInternalFrame ? ((JInternalFrame) frameOrPanel)
        .getContentPane()
        : frameOrPanel);
    JSVPanel jsvp = (JSVPanel) jp.getComponent(0);
    JDXSpectrum spectrum = jsvp.getSpectrum();
    if (spectrum.getIntegrationGraph() == null)
      return jsvp;
    spectrum.setIntegrationGraph(null);
    JSVPanel newJsvp = new JSVPanel(spectrum);
    jp.remove(jsvp);
    jp.add(newJsvp);
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

  public static JSVPanel checkIntegral(
                                       JSVPanel jsvp,
                                       Container frameOrPanel,
                                       int mode,
                                       boolean showMessage,
                                       ArrayList<IntegrationRatio> integrationRatios,
                                       Parameters parameters) {
    return (mode == JDXSpectrum.INTEGRATE_OFF
        || mode != JDXSpectrum.INTEGRATE_ON
        && jsvp.getSpectrum().getIntegrationGraph() != null 
        ? removeIntegration(frameOrPanel)
        : integrate(frameOrPanel, showMessage, integrationRatios, parameters));
  }

  public static void integrate(List<JDXSpectrum> specs,
                               ArrayList<IntegrationRatio> integrationRatios) {
    // TODO Auto-generated method stub
    
  }

  /**
   * Returns a <code>Color</code> from a string representation as a hex value or
   * a delimiter separated rgb values. The following are all valid arguments:
   * 
   * <pre>
   * "#ffffff"
   * "#FFFFFF"
   * "255 255 255"
   * "255,255,255"
   * "255;255;255"
   * "255-255-255"
   * "255.255.255"
   * </pre>
   * 
   * @param string
   *        the color as a string
   * @return a <code>Color</code> from a string representation
   */
  public static Color getColorFromString(String strColor) {
    return new Color(ColorUtil.getArgbFromString(strColor.trim()));
  }

}

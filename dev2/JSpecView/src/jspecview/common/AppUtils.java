package jspecview.common;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionListener;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import jspecview.common.IntegralGraph;
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
  public static AwtPanel integrate(
                                   Container frameOrPanel,
                                   boolean showDialog,
                                   Parameters parameters) {
    JPanel jp = (JPanel) (frameOrPanel instanceof JInternalFrame ? ((JInternalFrame) frameOrPanel)
        .getContentPane()
        : frameOrPanel);
    AwtPanel jsvp = (AwtPanel) jp.getComponent(0);
    JDXSpectrum spectrum = jsvp.getSpectrum();
    IntegralGraph graph = spectrum.getIntegrationGraph();
    spectrum.setIntegrationGraph(null);
    if (graph != null || spectrum.canIntegrate() && jsvp.getNumberOfSpectraInCurrentSet() == 1) {
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
        AwtPanel newJsvp = AwtPanel.getIntegralPanel(spectrum, jsvp
            .getIntegralPlotColor(), jsvp.getPopup());
        // add integration ratio annotations if any exist
        jp.remove(jsvp);
        jp.add(newJsvp);
      }
    }
    return (AwtPanel) jp.getComponent(0);
  }

  public static AwtPanel removeIntegration(Container frameOrPanel) {
    JPanel jp = (JPanel) (frameOrPanel instanceof JInternalFrame ? ((JInternalFrame) frameOrPanel)
        .getContentPane()
        : frameOrPanel);
    AwtPanel jsvp = (AwtPanel) jp.getComponent(0);
    JDXSpectrum spectrum = jsvp.getSpectrum();
    if (spectrum.getIntegrationGraph() == null)
      return jsvp;
    spectrum.setIntegrationGraph(null);
    AwtPanel newJsvp = new AwtPanel(spectrum, jsvp.getPopup());
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
    if (color == null)
      return "";
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

  public static AwtPanel checkIntegral(
                                       AwtPanel jsvp,
                                       Container frameOrPanel,
                                       Parameters parameters, String value) {
    IntegralGraph graph = jsvp.getSpectrum().getIntegrationGraph();
    boolean showMessage = false;//value.equals("?");
    int mode = IntegralGraph.getMode(value);
    if (mode == IntegralGraph.INTEGRATE_MARK) {
      if (graph == null) {
        jsvp = checkIntegral(jsvp, frameOrPanel, parameters, "ON");
        graph = jsvp.getSpectrum().getIntegrationGraph();
      }
      if (graph != null)
        graph.addMarks(value.substring(5).trim());
      return jsvp;    
    }
    return (mode == IntegralGraph.INTEGRATE_OFF
        || mode != IntegralGraph.INTEGRATE_ON && graph != null 
        ? removeIntegration(frameOrPanel)
        : integrate(frameOrPanel, showMessage, parameters));
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

  ////////////////// menuing methods common to app and applet //////////////
  
  
  public static void setMenus(JMenu saveAsMenu, JMenu saveAsJDXMenu,
                              JMenu exportAsMenu, ActionListener actionListener) {
    saveAsMenu.setText("Save As");
    saveAsJDXMenu.setText("JDX");
    addMenuItem(saveAsJDXMenu, "XY", actionListener);
    addMenuItem(saveAsJDXMenu, "DIF", actionListener);
    addMenuItem(saveAsJDXMenu, "DIFDUP", actionListener);
    addMenuItem(saveAsJDXMenu, "FIX", actionListener);
    addMenuItem(saveAsJDXMenu, "PAC", actionListener);
    addMenuItem(saveAsJDXMenu, "SQZ", actionListener);
    saveAsMenu.add(saveAsJDXMenu);
    addMenuItem(saveAsMenu, "CML", actionListener);
    addMenuItem(saveAsMenu, "XML (AnIML)", actionListener);
    if (exportAsMenu != null) {
      exportAsMenu.setText("Export As");
      addMenuItem(exportAsMenu, "JPG", actionListener);
      addMenuItem(exportAsMenu, "PNG", actionListener);
      addMenuItem(exportAsMenu, "SVG", actionListener);
    }
  }

  private static void addMenuItem(JMenu m, String key,
                                  ActionListener actionListener) {
    JMenuItem jmi = new JMenuItem();
    jmi.setMnemonic(key.charAt(0));
    jmi.setText(key);
    jmi.addActionListener(actionListener);
    m.add(jmi);
  }

  /**
   * setting of boolean parameters for a panel is only through Parameters
   * 
   * @param jsvp
   * @param params
   * @param st
   * @param b
   */
  public static void setBoolean(AwtPanel jsvp, Parameters params,
                                  ScriptToken st, boolean b) {
    if (params == null)
      params = new Parameters("temp");
    params.setBoolean(st, b);
    jsvp.setBoolean(params, st);
  }

  /**
   * Returns a randomly generated <code>Color</code>
   * 
   * @return a randomly generated <code>Color</code>
   */
  public static Color generateRandomColor() {
    while (true) {
      int red = (int) (Math.random() * 255);
      int green = (int) (Math.random() * 255);
      int blue = (int) (Math.random() * 255);
      Color randomColor = new Color(red, green, blue);
      if (!randomColor.equals(Color.blue))
        return randomColor;
    }
  }

  public static AwtPanel taConvert(AwtPanel jsvp, int mode) {
    if (jsvp.getNumberOfSpectraTotal() > 1)
      return null;
    JDXSpectrum spectrum = JDXSpectrum.taConvert(jsvp.getSpectrum(), mode);
    return (spectrum == jsvp.getSpectrum() ? null : new AwtPanel(spectrum,
        jsvp.popup));
  }

  public static void showSolutionColor(Component component, String sltnclr) {
    JOptionPane.showMessageDialog(component, "<HTML><body bgcolor=rgb("
        + sltnclr + ")><br />Predicted Solution Colour- RGB(" + sltnclr
        + ")<br /><br /></body></HTML>", "Predicted Colour",
        JOptionPane.INFORMATION_MESSAGE);
  }


}

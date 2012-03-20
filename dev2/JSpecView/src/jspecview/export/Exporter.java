package jspecview.export;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jspecview.common.JDXSpectrum;
import jspecview.common.JSVPanel;
import jspecview.common.JSpecViewFileFilter;
import jspecview.util.TextFormat;

public class Exporter {

  public enum Type {
    UNK, DIF, FIX, SQZ, PAC, XY, DIFDUP, PNG, JPG, SVG, SVGI, CML, AML;

    public static Type getType(String type) {
      type = type.toUpperCase();
      if (type.startsWith("XML"))
        return AML;
      for (Type mode : values())
        if (mode.name().equals(type)) 
          return mode;
      return UNK;
    }    
    
    public static boolean isExportMode(String ext) {
      return (getType(ext) != UNK);
    }
  }
  
  public Exporter() {
  }


  /**
   * returns message if path is not null, otherwise full string of text (unsigned applet)
   * 
   * @param type
   * @param path
   * @param spec
   * @param startIndex
   * @param endIndex
   * @return
   * @throws IOException
   */
  public static String exportTheSpectrum(Type mode, String path, 
                                         JDXSpectrum spec, int startIndex, int endIndex)
      throws IOException {
    switch (mode) {
    case XY:
    case DIF:
    case DIFDUP:
    case FIX:
    case PAC:
    case SQZ:
      return JDXExporter.export(mode, path, spec, startIndex, endIndex);      
    case SVG:
    case SVGI:
      return (new SVGExporter()).exportAsSVG(path, spec, startIndex, endIndex, mode == Type.SVGI);
    case CML:
      return (new CMLExporter()).exportAsCML(path, spec, startIndex, endIndex);
    case AML:
      return (new AMLExporter()).exportAsAnIML(path, spec, startIndex, endIndex);
    default:
      return null;
    }
  }

  public static String exportSpectra(JSVPanel panel, JFrame frame,
                                     JFileChooser fc, String type,
                                     String recentFileName,
                                     String dirLastExported) {
    // From popup menu click SaveAs or Export
    // if JSVPanel has more than one spectrum...Choose which one to export
    int numOfSpectra = panel.getNumberOfGraphSets();
    if (numOfSpectra == 1 || type.equals("JPG") || type.equals("PNG"))
      return exportSpectrumOrImage(panel, type, -1, fc, recentFileName,
          dirLastExported);

    String[] items = new String[numOfSpectra];
    for (int i = 0; i < numOfSpectra; i++)
      items[i] = panel.getSpectrumAt(i).getTitle();

    final JDialog dialog = new JDialog(frame, "Choose Spectrum", true);
    dialog.setResizable(false);
    dialog.setSize(200, 100);
    dialog.setLocation((panel.getLocation().x + panel.getSize().width) / 2,
        (panel.getLocation().y + panel.getSize().height) / 2);
    final JComboBox cb = new JComboBox(items);
    Dimension d = new Dimension(120, 25);
    cb.setPreferredSize(d);
    cb.setMaximumSize(d);
    cb.setMinimumSize(d);
    JPanel p = new JPanel(new FlowLayout());
    JButton button = new JButton("OK");
    p.add(cb);
    p.add(button);
    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(
        new JLabel("Choose Spectrum to export", SwingConstants.CENTER),
        BorderLayout.NORTH);
    dialog.getContentPane().add(p);
    String dir = dirLastExported;
    final String dl = dirLastExported;
    final String t = type;
    final String rfn = recentFileName;
    final JFileChooser f = fc;
    final JSVPanel jsvp = panel;
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int index = cb.getSelectedIndex();
        dialog.dispose();
        exportSpectrumOrImage(jsvp, t, index, f, rfn, dl);
      }
    });
    dialog.setVisible(true);
    return dir;
  }

  /**
   * from EXPORT command
   * 
   * @param tokens
   * 
   * @return message for status line
   */
  public static String exportCmd(JSVPanel jsvp, List<String> tokens, boolean forInkscape) {
    // MainFrame or applet EXPORT command
    String mode = "XY";
    String fileName = null;
    switch (tokens.size()) {
    default:
      return "EXPORT what?";
    case 1:
      fileName = TextFormat.trimQuotes(tokens.get(0));
      int pt = fileName.indexOf(".");
      if (pt < 0)
        return "EXPORT mode?";
      break;
    case 2:
      mode = tokens.get(0).toUpperCase();
      fileName = TextFormat.trimQuotes(tokens.get(1));
      break;
    }
    String ext = fileName.substring(fileName.lastIndexOf(".") + 1)
        .toUpperCase();
    if (ext.equals("JDX")) {
      if (mode == null)
        mode = "XY";
    } else if (Type.isExportMode(ext)) {
      mode = ext;
    } else if (Type.isExportMode(mode)){
      fileName += "."  + mode;
    }
    Type type = Type.getType(mode);
    if (forInkscape && type == Type.SVG)
      type = Type.SVGI;
    return exportSpectrumOrImage(jsvp, type, -1, fileName);
  }

  /**
   * Auxiliary Export method
   * 
   * @param spec
   *        the spectrum to export
   * @param fc
   *        file chooser to use
   * @param mode
   *        the format to export in
   * @param index
   *        the index of the spectrum
   * @param recentFileName
   * @param dirLastExported
   * @return dirLastExported
   */
  private static String exportSpectrumOrImage(JSVPanel jsvp, String mode,
                               int index, JFileChooser fc, String recentFileName,
                               String dirLastExported) {
    Type imode = Type.getType(mode);
    JSpecViewFileFilter filter = new JSpecViewFileFilter();
    //TODO: This is flawed. It assumes the file name has one and only one "." in it. 
    int pt = recentFileName.lastIndexOf(".");
    String name = (pt < 0 ? recentFileName : recentFileName.substring(0, pt));
    switch(imode) {
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      name += ".jdx";
      break;
    case AML:
      mode = "XML";
      // fall through
    default:
      filter.addExtension(mode);
      filter.setDescription(mode + " Files");
      name += "." + mode.toLowerCase();
    }
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(name));
    int returnVal = fc.showSaveDialog(jsvp);
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return dirLastExported;
    File file = fc.getSelectedFile();
    String dir = file.getParent();
    if (file.exists()) {
      int option = JOptionPane.showConfirmDialog(jsvp, "Overwrite file?",
          "Confirm Overwrite Existing File", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (option == JOptionPane.NO_OPTION)
        return dir;
    }
    exportSpectrumOrImage(jsvp, imode, index, file.getAbsolutePath());
    return dir;
  }
  
  private static String exportSpectrumOrImage(JSVPanel jsvp, Type imode,
                                              int index, String path) {
    JDXSpectrum spec;
    if (index < 0) {
      index = 0;
      spec = jsvp.getSpectrum();
    } else {
      spec = jsvp.getSpectrumAt(index);
    }
    int startIndex = jsvp.getStartDataPointIndices()[index];
    int endIndex = jsvp.getEndDataPointIndices()[index];
    String msg;
    try {
      switch (imode) {
      case PNG:
      case JPG:
        Image image = jsvp.createImage(jsvp.getWidth(), jsvp.getHeight());
        jsvp.paint(image.getGraphics());
        ImageIO.write((RenderedImage) image, (imode == Type.PNG ? "png" : "jpg"),
            new File(path));
        msg = " OK";
        break;
      case SVG:
      case SVGI:
        msg = (new SVGExporter()).exportAsSVG(path, spec.getXYCoords(), 
            spec.getTitle(), startIndex, endIndex, spec.getXUnits(), 
            spec.getYUnits(), spec.isContinuous(), spec.isIncreasing(), 
            jsvp.getPlotAreaColor(), jsvp.getBackground(), jsvp.getPlotColor(0),
            jsvp.getGridColor(), jsvp.getTitleColor(), jsvp.getScaleColor(),
            jsvp.getUnitsColor(), imode == Type.SVGI);
        break;
      default:
        msg = exportTheSpectrum(imode, path, spec, startIndex, endIndex);
      }
      return "Exported " + imode.name() + ": " + path + msg;
    } catch (IOException ioe) {
      return "Error exporting " + path + ": " + ioe.getMessage();
    }
  }
  
}

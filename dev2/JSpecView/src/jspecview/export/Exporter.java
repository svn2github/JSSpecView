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

  static final int UNK = -1;
  static final int DIF = 0;
  static final int FIX = 1;
  static final int SQZ = 2;
  static final int PAC = 3;
  static final int XY = 4;
  static final int DIFDUP = 5;
  static final int PNG = 6;
  static final int JPG = 7;
  static final int SVG = 8;
  static final int CML = 9;
  static final int AML = 10;

  private static final String TYPES = " DIF      " // 0
                                     +" FIX      " // 10
                                     +" SQZ      " // 20
                                     +" PAC      " // 30
                                     +" XY       " // 40
                                     +" DIFDUP   " // 50
                                     +" PNG      " // 60
                                     +" JPG      " // 70
                                     +" SVG      " // 80
                                     +" CML      " // 90
                                     +" AML      " //100
                                     +" AnIML    " //110
                                     +" XML      ";//120
  
  public Exporter() {
  }

  public static int getType(String type) {
    int pt = TYPES.indexOf(" " + type.toUpperCase() + " ");
    if (pt < 0)
      return UNK;
    return Math.min(pt / 10, AML);
  }
  
  public static String export(String type, String path, JDXSpectrum spec, int startIndex, int endIndex)
      throws IOException {
    int iType = getType(type);
    switch (iType) {
    case XY:
    case DIF:
    case DIFDUP:
    case FIX:
    case PAC:
    case SQZ:
      return JDXExporter.export(iType, path, spec, startIndex, endIndex);      
    case SVG:
      return (new SVGExporter()).exportAsSVG(path, spec, startIndex, endIndex);
    case CML:
      return (new CMLExporter()).exportAsCML(path, spec, startIndex, endIndex);
    case AML:
      return (new AMLExporter()).exportAsAnIML(path, spec, startIndex, endIndex);
    default:
      return null;
    }
  }

  public static boolean isExportMode(String ext) {
    return (getType(ext) >= 0);
  }

  public static String exportSpectra(JSVPanel panel, JFrame frame, JFileChooser fc, String type,
                              String recentFileName, String dirLastExported) {
    // if JSVPanel has more than one spectrum...Choose which one to export
    int numOfSpectra = panel.getNumberOfSpectra();
    if (numOfSpectra == 1 || type.equals("JPG") || type.equals("PNG"))
      return exportSpectrum(panel, type, -1, fc, recentFileName, dirLastExported);
  
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
        exportSpectrum(jsvp, t, index, f, rfn, dl);
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
  public static String export(JSVPanel jsvp, List<String> tokens) {
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
    } else if (isExportMode(ext)) {
      mode = ext;
    } else if (isExportMode(mode)){
      fileName += "."  + mode;
    }
    return export(jsvp, mode, -1, new File(fileName));
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
  private static String exportSpectrum(JSVPanel jsvp, String mode,
                               int index, JFileChooser fc, String recentFileName,
                               String dirLastExported) {
    JSpecViewFileFilter filter = new JSpecViewFileFilter();
    //TODO: This is flawed. It assumes the file name has one and only one "." in it. 
    String name = TextFormat.split(recentFileName, ".")[0];
    if ("XY FIX PAC SQZ DIF DIFDUP".indexOf(mode) >= 0) {
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      name += ".jdx";
    } else {
      if (mode.toLowerCase().indexOf("iml") >= 0
          || mode.toLowerCase().indexOf("aml") >= 0)
        mode = "XML";
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
    export(jsvp, mode, index, file);
    return dir;
  }
  
  private static String export(JSVPanel jsvp, String mode, int index, File file) {
    JDXSpectrum spec;
    if (index < 0) {
      index = 0;
      spec = jsvp.getSpectrum();
    } else {
      spec = jsvp.getSpectrumAt(index);
    }
    int startIndex = jsvp.getStartDataPointIndices()[index];
    int endIndex = jsvp.getEndDataPointIndices()[index];
    String msg = " OK";
    try {
      if (mode.equals("PNG") || mode.equals("JPG")) {
        try {
          Image image = jsvp.createImage(jsvp.getWidth(), jsvp.getHeight());
          jsvp.paint(image.getGraphics());
          ImageIO.write((RenderedImage) image, mode.toLowerCase(), new File(file
              .getAbsolutePath()));
        } catch (IOException ioe) {
          ioe.printStackTrace();
          msg = "Error: " + ioe.getMessage();
        }
      } else if (mode.equals("SVG")) {
        msg = (new SVGExporter()).exportAsSVG(file.getAbsolutePath(), spec.getXYCoords(), spec.getTitle(), startIndex,
            endIndex, spec.getXUnits(), spec.getYUnits(), spec.isContinuous(),
            spec.isIncreasing(), jsvp.getPlotAreaColor(), jsvp.getBackground(),
            jsvp.getPlotColor(0), jsvp.getGridColor(), jsvp.getTitleColor(), 
            jsvp.getScaleColor(), jsvp.getUnitsColor(), jsvp.isSvgExportForInkscapeEnabled());
      } else {
        msg = export(mode, file.getAbsolutePath(), spec, startIndex,
            endIndex);
      }
      return "Exported " + mode + ": " + file.getAbsolutePath() + msg;
    } catch (IOException ioe) {
      return "Error exporting " + file.getAbsolutePath() + ": " + ioe.getMessage();
    }
  }
}

package jspecview.export;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import jspecview.common.JSVFileFilter;
import jspecview.common.JSVPanel;
import jspecview.common.PanelData;
import jspecview.common.ScriptInterface;
import jspecview.common.ScriptToken;
import jspecview.util.FileManager;
import jspecview.util.TextFormat;

public class Exporter {

  public final static String sourceLabel = "Original...";
  public enum Type {
    UNK, SOURCE, DIF, FIX, SQZ, PAC, XY, DIFDUP, PNG, JPG, SVG, SVGI, CML, AML;

    public static Type getType(String type) {
      type = type.toUpperCase();
      if (type.equalsIgnoreCase(sourceLabel))
        return SOURCE;
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
   * @param mode 
   * @param path
   * @param spec
   * @param startIndex
   * @param endIndex
   * @return message or text
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

  /**
   * 
   * @param si 
   * @param frame
   * @param fc
   * @param type
   * @param dirLastExported
   * @return null or directory saved to
   */
  public static String exportSpectra(ScriptInterface si, JFrame frame,
                                     JFileChooser fc, String type,
                                     String dirLastExported) {
  	
  	JSVPanel jsvp = si.getSelectedPanel();
  	boolean isView = si.getCurrentSource().isView;
    // From popup menu click SaveAs or Export
    // if JSVPanel has more than one spectrum...Choose which one to export
    int nSpectra = jsvp.getPanelData().getNumberOfSpectraInCurrentSet();
    if (nSpectra == 1 
    		|| type.equals("JPG") 
    		|| type.equals("PNG")
    		|| !isView && type.equals(sourceLabel) 
    		|| jsvp.getPanelData().getCurrentSpectrumIndex() >= 0 
    		)
      return exportSpectrumOrImage(si, type, -1, fc, dirLastExported);
    
    
    String[] items = new String[nSpectra];
    for (int i = 0; i < nSpectra; i++)
      items[i] = jsvp.getSpectrumAt(i).getTitle();

    final JDialog dialog = new JDialog(frame, "Choose Spectrum", true);
    dialog.setResizable(false);
    dialog.setSize(200, 100);
    Component panel = (Component) jsvp;
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
    final int ret[] = new int[] { -1 };
    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ret[0] = cb.getSelectedIndex();
        dialog.dispose();
      }
    });
    dialog.setVisible(true);
    dialog.dispose();
    if (ret[0] < 0)
    	return null;    
    String msg = exportSpectrumOrImage(si, type, ret[0], fc, dirLastExported);
    return (msg == null ? null : dir);
    
  }

  /**
   * from EXPORT command
   * @param jsvp 
   * @param tokens
   * @param forInkscape 
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
   * @param si 
   * @param mode
   *        the format to export in
   * @param index
   *        the index of the spectrum
   * @param fc
   *        file chooser to use
   * @param dirLastExported
   * @return dirLastExported
   */
	private static String exportSpectrumOrImage(ScriptInterface si,
                                              String mode, int index,
                                              JFileChooser fc,
                                              String dirLastExported) {
		JSVPanel jsvp = si.getSelectedPanel();
    Type imode = Type.getType(mode);
    JSVFileFilter filter = new JSVFileFilter();
    String sourcePath = jsvp.getSpectrum().getFilePath();
    String newName = FileManager.getName(sourcePath);

    int pt = newName.lastIndexOf(".");
    String name = (pt < 0 ? newName : newName.substring(0, pt));
    if (si.getCurrentSource().isView)
    	name += si.getCurrentSource().getFilePath();
    switch (imode) {
    case XY:
    case FIX:
    case PAC:
    case SQZ:
    case DIF:
    case DIFDUP:
    case SOURCE:
      filter.addExtension("jdx");
      filter.addExtension("dx");
      filter.setDescription("JCAMP-DX Files");
      name += ".jdx";
      break;
    case AML:
      mode = "XML";
      //$FALL-THROUGH$
    default:
      filter.addExtension(mode);
      filter.setDescription(mode + " Files");
      name += "." + mode.toLowerCase();
    }
    fc.setFileFilter(filter);
    fc.setSelectedFile(new File(name));
    int returnVal = fc.showSaveDialog((Component) jsvp);
    if (returnVal != JFileChooser.APPROVE_OPTION)
      return dirLastExported;
    File file = fc.getSelectedFile();
    String dir = file.getParent();
    if (file.exists()) {
      int option = JOptionPane.showConfirmDialog((Component) jsvp,
          "Overwrite " + file.getName() + "?", "Confirm Overwrite Existing File",
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (option == JOptionPane.NO_OPTION)
        return dir;
    }
    String msg = "OK";
    if (imode == Type.SOURCE)
      FileManager.fileCopy(sourcePath, file);
    else
      msg = exportSpectrumOrImage(jsvp, imode, index, file
          .getAbsolutePath());
    if (msg != null)
    	si.updateRecentMenus(file.getAbsolutePath());
    return (msg == null ? null : dir);
  }
  
  /**
   * 
   * @param jsvp
   * @param imode
   * @param index
   * @param path
   * @return  status line message
   */
  private static String exportSpectrumOrImage(JSVPanel jsvp, Type imode,
                                              int index, String path) {
    JDXSpectrum spec;
    PanelData pd = jsvp.getPanelData();
    if (index < 0 && (index = pd.getCurrentSpectrumIndex()) < 0)
      return "ERROR: No spectrum selected";
    spec = pd.getSpectrumAt(index);
    int startIndex = pd.getStartDataPointIndices()[index];
    int endIndex = pd.getEndDataPointIndices()[index];
    String msg;
    try {
      switch (imode) {
      case PNG:
      case JPG:
        Image image = ((Component) jsvp).createImage(jsvp.getWidth(), jsvp.getHeight());
        ((Component) jsvp).paint(image.getGraphics());
        ImageIO.write((RenderedImage) image, (imode == Type.PNG ? "png" : "jpg"),
            new File(path));
        msg = " OK";
        break;
      case SVG:
      case SVGI:
        msg = (new SVGExporter()).exportAsSVG(path, spec.getXYCoords(), 
            spec.getTitle(), startIndex, endIndex, spec.getXUnits(), 
            spec.getYUnits(), spec.isContinuous(), spec.isXIncreasing(), spec.isInverted(), 
            (Color)jsvp.getColor(ScriptToken.PLOTAREACOLOR), ((Component) jsvp).getBackground(), 
            (Color)jsvp.getPlotColor(0),
            (Color)jsvp.getColor(ScriptToken.GRIDCOLOR),
            (Color)jsvp.getColor(ScriptToken.TITLECOLOR),
            (Color)jsvp.getColor(ScriptToken.SCALECOLOR),
            (Color)jsvp.getColor(ScriptToken.UNITSCOLOR),
            imode == Type.SVGI);
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

package jspecview.java;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JButton;
//import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import org.jmol.io.Base64;

import jspecview.api.ScriptInterface;
import jspecview.common.JSVPanel;
import jspecview.common.PrintLayout;
import jspecview.common.Annotation.AType;
import jspecview.export.Exporter;
import jspecview.export.Exporter.ExportType;

/**
 * just a class I made to separate the construction of the AnnotationDialogs
 * from their use
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */
public class AwtDialogHelper {

	private String thisKey;
	private ActionListener eventListener;
	private Map<String, Object> options;
	private JPanel leftPanel;
	private Insets buttonInsets = new Insets(5, 5, 5, 5);
	private Insets cbInsets = new Insets(0, 0, 2, 2);
	private int iRow;
	private ScriptInterface si;

	public AwtDialogHelper(ScriptInterface si) {
		this.si = si;
	}

	
	public AwtDialogHelper(String thisKey, Map<String, Object> options,
			JPanel leftPanel, ActionListener eventListener) {
		this.thisKey = thisKey;
		this.options = options;
		this.leftPanel = leftPanel;
		this.eventListener = eventListener;
	}

	protected JButton addButton(String text, ActionListener a) {
		JButton	btn = new JButton();
		btn.setPreferredSize(new Dimension(120, 25));
		btn.setText(text);
		btn.addActionListener(a);
		leftPanel.add(btn, new GridBagConstraints(0, iRow++, 3, 1, 0.0,
				0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets,
				0, 0));
		return btn;
	}
//
//	protected JCheckBox addCheckBoxOption(String name, String label,
//			boolean isSelected) {
//		JCheckBox obj = new JCheckBox();
//		obj.setText(label == null ? name : label);
//		obj.addActionListener(eventListener);
//		leftPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
//				GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets, 0, 0));
//		iRow++;
//		return obj;
//	}

	protected JTextField addInputOption(String name, String label, String value,
			String units, String defaultValue, boolean visible) {
		String key = thisKey + "_" + name;
		if (value == null) {
			value = (String) options.get(key);
			if (value == null)
				options.put(key, (value = defaultValue));
		}
		JTextField obj = new JTextField(value);
		if (visible) {
			obj.setPreferredSize(new Dimension(75, 25));
			obj.addActionListener(eventListener);
  		addPanelLine(name, label, obj, units);
		}
		return obj;
	}

	protected JComboBox<String> addSelectOption(String name, String label, String[] info,
			int iPt, boolean visible) {
		JComboBox<String> obj = new JComboBox<String>(info);
		obj.setSelectedIndex(iPt);
		if (visible) {
			obj.setActionCommand(name);
			// obj.setPreferredSize(new Dimension(100, 25));
			obj.addActionListener(eventListener);
			addPanelLine(name, label, obj, null);
		}
		return obj;
	}

	private void addPanelLine(String name, String label, JComponent obj,
			String units) {
		leftPanel.add(new JLabel(label == null ? name : label),
				new GridBagConstraints(0, iRow, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, cbInsets, 0, 0));
		if (units == null) {
			leftPanel.add(obj, new GridBagConstraints(1, iRow, 2, 1, 0.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets, 0, 0));
		} else {
			leftPanel.add(obj, new GridBagConstraints(1, iRow, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE, cbInsets, 0, 0));
			leftPanel.add(new JLabel(units), new GridBagConstraints(2, iRow, 1, 1,
					0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, cbInsets,
					0, 0));
		}
		iRow++;
	}

	protected synchronized JTable getDataTable(AwtAnnotationDialog ad, 
			String[][] data, String[] columnNames, int[] columnWidths, int height) {
		
		AwtDialogTableModel tableModel = new AwtDialogTableModel(columnNames, data, true);
		JTable table = new JTable(tableModel);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setDefaultRenderer(String.class, new TitleRenderer());
    table.setCellSelectionEnabled(true);
    table.getSelectionModel().addListSelectionListener(ad);
    ad.columnSelector = table.getColumnModel().getSelectionModel();
    ad.columnSelector.addListSelectionListener(ad);
		int n = 0;
		for (int i = 0; i < columnNames.length; i++) {
			table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
			n += columnWidths[i];
		}
		table.setPreferredScrollableViewportSize(new Dimension(n, height));
		return table;
	}

  /**
   * TableCellRenderer that aligns text in the center of a JTable Cell
   */
  class TitleRenderer extends JLabel
                      implements TableCellRenderer {
      /**
     * 
     */
    private static final long serialVersionUID = 1L;


      public TitleRenderer(){
        setOpaque(true);
      }


      public Component getTableCellRendererComponent(
                              JTable table, Object title,
                              boolean isSelected, boolean hasFocus,
                              int row, int column) {
          setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.RIGHT);
          setText(title.toString());
          if(isSelected)
            setBackground(table.getSelectionBackground());
          else
            setBackground(table.getBackground());

          return this;
      }
  }
	private PrintLayout lastPrintLayout;
	private JFileChooser fc;
	public String dirLastOpened;
	public boolean useDirLastOpened;
	public boolean useDirLastExported;
	public String dirLastExported;

	private void saveImage(ExportType itype) {
  	JSVPanel jsvp = si.getSelectedPanel();
		setFileChooser(itype);
		String name = Exporter.getSuggestedFileName(si, itype);
		File file = getFile(name, jsvp, true);
		if (file == null)
			return;
    Image image = ((Component) jsvp).createImage(jsvp.getWidth(), jsvp.getHeight());
    ((Component) jsvp).paint(image.getGraphics());
    try {
			ImageIO.write((RenderedImage) image, itype.toString().toLowerCase(), file);
		} catch (IOException e) {
			jsvp.showMessage(e.getMessage(), "Error Saving Image");
		}
	}

	public String print(ScriptInterface si, Frame frame, String pdfFileName) {
		if (!si.isSigned())
			return "Error: Applet must be signed for the PRINT command.";
		boolean isJob = (pdfFileName == null || pdfFileName.length() == 0);
		boolean isBase64 = (!isJob && pdfFileName.toLowerCase().startsWith("base64"));
		JSVPanel jsvp = si.getSelectedPanel();
		if (jsvp == null)
			return null;
    jsvp.getPanelData().closeAllDialogsExcept(AType.NONE);
		PrintLayout pl = new AwtDialogPrint(frame, lastPrintLayout, isJob).getPrintLayout();
		if (pl == null)
			return null;
		lastPrintLayout = pl;
		if (isJob && pl.asPDF) {
			isJob = false;
			pdfFileName = "PDF";
		}		
		if (!isBase64 && !isJob) {
			setFileChooser(ExportType.PDF);
			if (pdfFileName.equals("?") || pdfFileName.equalsIgnoreCase("PDF"))
  			pdfFileName = Exporter.getSuggestedFileName(si, ExportType.PDF);
			File file = getFile(pdfFileName, jsvp, true);
			if (file == null)
				return null;
			si.setProperty("directoryLastExporteFile", dirLastExported = file.getParent());
			pdfFileName = file.getAbsolutePath();
		}
		String s = null;
		try {
			OutputStream os = (isJob ? null : isBase64 ? new ByteArrayOutputStream() 
			    : new FileOutputStream(pdfFileName));
			String printJobTitle = jsvp.getPanelData().getPrintJobTitle(true);
			if (pl.showTitle) {
				printJobTitle = jsvp.getInput("Title?", "Title for Printing", printJobTitle);
				if (printJobTitle == null)
					return null;
			}
			((AwtPanel) jsvp).printPanel(pl, os, printJobTitle);
			s = (isBase64 ? Base64.getBase64(
					((ByteArrayOutputStream) os).toByteArray()).toString() : "OK");
		} catch (Exception e) {
			jsvp.showMessage(e.getMessage(), "File Error");
		}
		return s;
	}

	public void setFileChooser(ExportType imode) {
		if (fc == null)
		  fc = new JFileChooser();
    AwtDialogFileFilter filter = new AwtDialogFileFilter();
    fc.resetChoosableFileFilters();
    switch (imode) {
    case UNK:
  		filter = new AwtDialogFileFilter();
  		filter.addExtension("xml");
  		filter.addExtension("aml");
  		filter.addExtension("cml");
  		filter.setDescription("CML/XML Files");
  		fc.setFileFilter(filter);
  		filter = new AwtDialogFileFilter();
  		filter.addExtension("jdx");
  		filter.addExtension("dx");
  		filter.setDescription("JCAMP-DX Files");
  		fc.setFileFilter(filter);
    	break;
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
      break;
    default:
      filter.addExtension(imode.toString().toLowerCase());
      filter.setDescription(imode + " Files");
    }
    fc.setFileFilter(filter);    
	}

	public File showFileOpenDialog(Frame frame) {
		setFileChooser(ExportType.UNK);
		return getFile("", frame, false);
	}


	public void exportSpectrum(JFrame frame, String type) {
		JSVPanel jsvp = si.getSelectedPanel();
		if (jsvp == null)
			return;
		ExportType eType = ExportType.getType(type);
		switch (eType) {
		case PDF:
			print(si, frame, "PDF");
			break;
		case PNG:
		case JPG:
			saveImage(eType);
			break;
		default:
			exportSpectrumAsk(si, frame, eType);
			jsvp.getFocusNow(true);
		}
	}

	/**
	 * 
	 * @param si
	 * @param frame
	 * @param eType
	 * @return directory saved to or a message starting with "Error:"
	 */
	private String exportSpectrumAsk(ScriptInterface si, Object frame, ExportType eType) {
		setFileChooser(eType);
		String[] items = Exporter.getExportableItems(si, eType.equals(ExportType.SOURCE));
		JSVPanel jsvp = si.getSelectedPanel();
		int index = (items == null ? -1 : jsvp.geOptionFromDialog(frame, items, "Export", "Choose a spectrum to export"));
		if (index == Integer.MIN_VALUE)
			return null;
		File file = getFile(Exporter.getSuggestedFileName(si, eType), jsvp, true);
		return Exporter.exportSpectrum(si, eType, index, file);
	}

	public File getFile(String name, Object panelOrFrame, boolean isSave) {
		Component c = (Component) panelOrFrame;
		fc.setSelectedFile(new File(name));
		if (isSave) {
			if (useDirLastExported)
				fc.setCurrentDirectory(new File(dirLastExported));
		} else {
			if (useDirLastOpened)
				fc.setCurrentDirectory(new File(dirLastOpened));
		}
		int returnVal = (isSave ? fc.showSaveDialog(c) : fc.showOpenDialog(c));
		if (returnVal != JFileChooser.APPROVE_OPTION)
			return null;
		File file = fc.getSelectedFile();
		if (isSave) {
			si.setProperty("directoryLastExportedFile", dirLastExported = file.getParent());
	    if (file.exists()) {
	      int option = JOptionPane.showConfirmDialog(c,
	          "Overwrite " + file.getName() + "?", "Confirm Overwrite Existing File",
	          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	      if (option == JOptionPane.NO_OPTION)
	        return null;
	    }
		} else {
			si.setProperty("directoryLastOpenedFile", dirLastOpened = file.getParent());
		}
		return file;
	}

	public int geOptionFromDialog(Object frame, String[] items,
			JSVPanel jsvp, String dialogName, String labelName) {
		final JDialog dialog = new JDialog((JFrame) frame, dialogName, true);
		dialog.setResizable(false);
		dialog.setSize(200, 100);
		Component panel = (Component) jsvp;
		dialog.setLocation((panel.getLocation().x + panel.getSize().width) / 2,
				(panel.getLocation().y + panel.getSize().height) / 2);
		final JComboBox<Object> cb = new JComboBox<Object>(items);
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
				new JLabel(labelName, SwingConstants.CENTER),
				BorderLayout.NORTH);
		dialog.getContentPane().add(p);
		final int ret[] = new int[] { Integer.MIN_VALUE };
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ret[0] = cb.getSelectedIndex();
				dialog.dispose();
			}
		});
		dialog.setVisible(true);
		dialog.dispose();
		return ret[0];
	}


}

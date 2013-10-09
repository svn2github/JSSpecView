/* Copyright (c) 2002-2012 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.java;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;


import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jmol.util.Logger;

import jspecview.common.JDXSpectrum;
import jspecview.source.JDXSource;
import jspecview.util.JSVFileManager;

/**
 * Dialog that displays String of text or contents of a file in a
 * </code>JEditorPane</code>.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class AwtDialogText extends JDialog {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JPanel contentPanel = new JPanel();
  private BorderLayout borderLayout = new BorderLayout();
  private JScrollPane scrollPane;
  private JEditorPane sourcePane = new JEditorPane();
 	
  /**
   * Intilialises a <code>TextDialog</code> with a Reader from which to read the
   * pane content
   * @param frame the parent frame
   * @param title the title
   * @param modal true if modal, otherwise false
   * @param text
   */
  public AwtDialogText(Frame frame, String title, boolean modal, String text) {
    super(frame, title, modal);
    try {
      jbInit(text);
      //setSize(500, 400);
      pack();
      setVisible(true);
    }
    catch(Exception ex) {
    	Logger.error(ex.getMessage());
    }
  }

  void jbInit(String text) throws Exception {
    contentPanel.setLayout(borderLayout);
    sourcePane.setText(text);
    sourcePane.setEditable(false);
    sourcePane.setFont(new Font(null, Font.BOLD, 12));
    getContentPane().add(contentPanel);
    scrollPane = new JScrollPane(sourcePane);
    scrollPane.setPreferredSize(new Dimension(500, 400));
    scrollPane.setMinimumSize(new Dimension(500, 400));
    contentPanel.add(scrollPane,  BorderLayout.CENTER);
  }

  public static void showProperties(Frame frame, JDXSpectrum spectrum) {
    Object[][] rowData = spectrum.getHeaderRowDataAsArray();
    String[] columnNames = { "Label", "Description" };
    JTable table = new JTable(rowData, columnNames);
    table.setPreferredScrollableViewportSize(new Dimension(400, 195));
    JScrollPane scrollPane = new JScrollPane(table);
    JOptionPane.showMessageDialog(frame, scrollPane, "Header Information",
        JOptionPane.PLAIN_MESSAGE);
  }

  
	public static void showSource(Frame frame, JDXSource currentSource) {
    if (currentSource == null) {
      JOptionPane.showMessageDialog(frame, "Please Select a Spectrum",
          "Select Spectrum", JOptionPane.WARNING_MESSAGE);
      return;
    }
		try {
			String f = currentSource.getFilePath();
			new AwtDialogText(frame, f, true, JSVFileManager.getFileAsString(f, null));
		} catch (Exception ex) {
			new AwtDialogText(frame, "File Not Found", true, "File Not Found");
		}
	}

  public static void showError(Frame frame, JDXSource currentSource) {
    if (currentSource == null) {
      JOptionPane.showMessageDialog(frame, "Please Select a Spectrum",
          "Select Spectrum", JOptionPane.WARNING_MESSAGE);
      return;
    }
    String errorLog = currentSource.getErrorLog();
    if (errorLog != null)
      new AwtDialogText(frame, currentSource.getFilePath(), true, errorLog);
  }
}

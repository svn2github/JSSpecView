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

package jspecview.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import jspecview.common.Annotation.AType;
import jspecview.common.AnnotationData;
import jspecview.util.TextFormat;

/**
 * Dialog for managing peak, integral, and measurement listings
 * for a Spectrum within a GraphSet
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
abstract class AnnotationDialog extends JDialog implements AnnotationData {

	private static final long serialVersionUID = 1L;
	
	abstract protected void addControls();
	abstract protected void checkEnables();
	abstract protected AnnotationData createData();	
	abstract protected int[] getPosXY();
	abstract protected void updateValues();
	
	protected AType thisType;
	protected String subType;
	
	protected ScriptInterface si;
	protected JSVPanel jsvp;
	protected JDXSpectrum spec;
	
	protected Map<String, Object> data;	

	protected String thisKey;
  
	private JPanel leftPanel, rightPanel;
	protected JButton showHideButton;

	private JButton clearButton, applyButton, doneButton;	
	protected final static Map<String, Object> options = new HashMap<String, Object>();

	private Object[] myOptions;
	private String[] unitOptions;
	private String[] formatOptions;

	private Integer unitPtr;
	protected JTextField txtFormat;
	protected JTextField txtFontSize;
	protected JComboBox cmbUnits;
	

	/**
	 * Initialises the <code>IntegralDialog</code> with the given values for minY,
	 * offset and factor
	 * 
	 * @param jsvp
	 *          the parent panel
	 * @param spectraTree
	 * @param modal
	 *          the modality
	 */
	protected AnnotationDialog(String title, ScriptInterface si, JDXSpectrum spec, 
			JSVPanel jsvp, Map<String, Object> data) {
		this.si = si;
		this.jsvp = jsvp;
		this.spec = spec;
		this.data = data;
		setModal(false);
		setPosition((Component)jsvp, getPosXY());
		setResizable(true);
		// after specific constructor, run setup()
	}

	private void setPosition(Component panel, int[] posXY) {
		if (panel != null) {
			if (posXY[0] == Integer.MIN_VALUE) {
				posXY[0] = panel.getLocationOnScreen().x;
				posXY[1] = panel.getLocationOnScreen().y + panel.getHeight() - 20;
			}
			setLocation(posXY[0], posXY[1]);
		}
	}

	ActionListener eventListener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      doEvent(e);
    }
  };


	protected DialogHelper dialogHelper;
	protected JTable dataTable;
	
	protected void setup() {
		getContentPane().removeAll();
		subType = spec.getTypeLabel();
		thisKey = thisType + "_" + subType;
		myOptions = (Object[]) options.get(thisKey);
		if (myOptions == null)
			options.put(thisKey, myOptions = JDXDataObject.getDefaultAnnotationInfo(
					spec, thisType));
		unitOptions = (String[]) myOptions[0];
		formatOptions = (String[]) myOptions[1];
		unitPtr = (Integer) options.get(thisKey + "_unitPtr");
		if (unitPtr == null)
			unitPtr = (Integer) myOptions[2];

		try {
			jbInit();
			pack();
			setVisible(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

  void jbInit() throws Exception {

		showHideButton = newJButton();
    showHideButton.setText("Show");
    showHideButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	JButton b = (JButton) e.getSource();
				showHide(b.getText().equals("Show"));
      }
    });
    
  	clearButton = newJButton();
    clearButton.setText("Clear");
    clearButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clear();
      }
    });
    
  	applyButton = newJButton();
    applyButton.setText("Apply");
    applyButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        apply();
      }
    });
    
    doneButton = newJButton();
    doneButton.setText("Done");
    doneButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        done();
      }
    });

    leftPanel = new JPanel(new GridBagLayout());
		dialogHelper = new DialogHelper(thisKey, options, leftPanel, eventListener);
    addTopControls();
    addControls();
    leftPanel.setMinimumSize(new Dimension(150, 300));
    dialogHelper.addButton(applyButton);
    dialogHelper.addButton(showHideButton);
    dialogHelper.addButton(clearButton);
    dialogHelper.addButton(doneButton);
    dialogHelper = null;
        
    rightPanel = new JPanel();
  	JScrollPane scrollPane = new JScrollPane(rightPanel);

    JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setResizeWeight(0);
    mainSplitPane.setRightComponent(scrollPane);
    mainSplitPane.setLeftComponent(leftPanel);

    setPreferredSize(new Dimension(500,350));
    getContentPane().removeAll();
    getContentPane().add(mainSplitPane);
    
    checkEnables();
  }

	protected void loadData(String[] header, String[][] data) {
		try {
		rightPanel.removeAll();
		rightPanel.add(dataTable = (new DialogHelper()).getDataTable(jsvp, header, data));
		} catch (Exception e) {
			// not perfect.
		}
		validate();
		repaint();
	}

	protected JButton newJButton() {
		JButton b = new JButton();
		b.setPreferredSize(new Dimension(120,25));
		return b;
	}

	private void addTopControls() {
		
		if (unitPtr != null)
  		cmbUnits = dialogHelper.addSelectOption("Units", null, unitOptions, unitPtr.intValue());
		
		String key = thisKey + "_format";
		String format = (String) options.get(key);
		if (format == null)
			options.put(key, (format = formatOptions[unitPtr == null ? 0 : unitPtr.intValue()]));
		txtFormat = dialogHelper.addInputOption("numberFormat", "Number Format", format, null, null);	
		//txtFontSize = ((DialogHelper dialogHelper)).addInputOption("FontSize", "Font Size", null, null, "10");
	}
	
	protected void showHide(boolean isShow) {
		setState(isShow);
		if (isShow)
			apply();
		
	  //JSViewer.runScriptNow(si, "show" + thisType + (isShow ? " true" : " false"));
	  checkEnables();
	}

	protected void clear() {
  	//something here
  	updateValues();
	}
	
  protected void done() {
  	jsvp.getPanelData().addDialog(-1, thisType, this);
  	dispose();
	}

	protected void doEvent(ActionEvent e) {
		if (e.getActionCommand().equals("Units")) {
			txtFormat.setText(formatOptions[cmbUnits.getSelectedIndex()]);
			return;
		}
		if (e.getSource() instanceof JTextField) {
			apply();
			return;
		}
		
	}

	public void reEnable() {
		updateValues();
  	checkEnables();
		setVisible(true);
	}
	
	public void apply() {
		updateValues();
  	checkEnables();
  	jsvp.repaint();
	}
	
	private boolean isON = true;
	
	public boolean getState() {
	  return isON;	
	}
	
	public void setState(boolean b) {
		isON = b;
	}
	

	protected Parameters myParams = new Parameters("MeasurementData");
	public Parameters getParameters() {
		return myParams;
	}

	protected List<Measurement> measurements = new ArrayList<Measurement>();
	
	public List<Measurement> getMeasurements() {
		return measurements;
	}
	
	public void setMeasurements(List<Measurement> measurements) {
		this.measurements = measurements;
	}

	public AType getType() {
		return thisType;
	}

	public JDXSpectrum getSpectrum() {
		return spec;
	}

	protected AnnotationData xyData;
	protected DecimalFormat numberFormatter;
	
	public AnnotationData getData() {
		if (xyData == null)
			xyData = createData();
	  return xyData;	
	}
	
	public void setData(AnnotationData data) {
		xyData = data;
	}

	public void addSpecShift(double dx) {
		if (xyData != null)
			xyData.addSpecShift(dx);
	}

	
	protected void setParams() {
		 myParams.numberFormat = txtFormat.getText();
			numberFormatter = TextFormat.getDecimalFormat("#" + myParams.numberFormat);
	 
	}
	
	public void update(Coordinate clicked) {
	}

}

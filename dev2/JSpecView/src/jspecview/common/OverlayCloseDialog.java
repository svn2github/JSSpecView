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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;

/**
 * Dialog for managing overlaying spectra and closing files
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class OverlayCloseDialog extends JDialog implements WindowListener {

	private static final long serialVersionUID = 1L;
	private ScriptInterface si;
	private List<JSVTreeNode> treeNodes;
	private List<JCheckBox> checkBoxes;
	private JPanel spectrumPanel;
	private Insets cbInsets1;
	private Insets cbInsets2;
	private JButton closeSelectedButton;
	private Rectangle bounds;
	private JButton overlaySelectedButton;
  
  
  /**
    * Initialises the <code>IntegralDialog</code> with the given values for minY, offset
    * and factor
    * @param panel the parent panel
    * @param spectraTree
    * @param modal the modality
   */
  public OverlayCloseDialog(ScriptInterface si, JSVSpectrumPanel panel, boolean modal) {
  	this.si = si;
    this.setTitle("Overlay/Close Spectra");
    this.setModal(modal);
    if(panel != null) {
      setLocation( panel.getLocationOnScreen().x,
                   panel.getLocationOnScreen().y);
      panel.markSelectedPanels(si.getSpecNodes());
    }
    setResizable(true);
    addWindowListener(this);
    setup();
  }

	private void setup() {
    try {
      jbInit();
      pack();
      if (bounds != null)
      	setBounds(bounds);
      setVisible(true);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    //bounds = getBounds();
	}

  void jbInit() throws Exception {
    layoutCheckBoxes((JTree) si.getSpectraTree());
        
    JButton selectAllButton = new JButton();
    JButton selectNoneButton = new JButton();
    overlaySelectedButton = new JButton();
    closeSelectedButton = new JButton();
    JButton doneButton = new JButton();

    selectAllButton.setText("Select All");
    selectAllButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        select(true);
      }
    });
    
    selectNoneButton.setText("Select None");
    selectNoneButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        select(false);
      }
    });
    
    overlaySelectedButton.setText("Overlay Selected");
    overlaySelectedButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	//bounds = getBounds();
        overlaySelected();
      }
    });
    
    closeSelectedButton.setText("Close Selected");
    closeSelectedButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	//bounds = getBounds();
        closeSelected();
      }
    });
    
    doneButton.setText("Done");
    doneButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        done();
      }
    });

    Insets buttonInsets = new Insets(5, 5, 5, 5);

    JPanel midPanel = new JPanel(new GridBagLayout());
    midPanel.add(selectAllButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
    		GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets, 0, 0));
    
    midPanel.add(selectNoneButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
    		GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets, 0, 0));
    

    JPanel buttonPanel = new JPanel(new GridBagLayout());
    buttonPanel.add(overlaySelectedButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
    		GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets, 0, 0));
    
    buttonPanel.add(closeSelectedButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
    		GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets, 0, 0));
    
    buttonPanel.add(doneButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
    		GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets, 0, 0));
        
    //GridBagConstraints(int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, 
    		// int anchor, int fill, Insets insets, int ipadx, int ipady)

    JPanel lowerPanel = new JPanel(new BorderLayout());
    lowerPanel.add(midPanel, BorderLayout.NORTH);
    lowerPanel.add(buttonPanel, BorderLayout.SOUTH);

    JScrollPane scrollPane = new JScrollPane(spectrumPanel);

    JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setResizeWeight(1);
    mainSplitPane.setTopComponent(scrollPane);
    mainSplitPane.setBottomComponent(lowerPanel);

    setPreferredSize(new Dimension(350,500));
    getContentPane().removeAll();
    getContentPane().add(mainSplitPane, BorderLayout.CENTER);


    //getContentPane().add(mainPanel);
    checkEnables();
  }

	private void layoutCheckBoxes(JTree spectraTree) {
    checkBoxes = new ArrayList<JCheckBox>();
    treeNodes = new ArrayList<JSVTreeNode>();
    cbInsets1 = new Insets(0, 0, 2, 2);
    cbInsets2 = new Insets(0, 20, 2, 2);
		spectrumPanel = new JPanel(new GridBagLayout());
    addCheckBoxes((JSVTreeNode) si.getRootNode(), 0);
	}

	@SuppressWarnings("unchecked")
	private void addCheckBoxes(JSVTreeNode rootNode, int level) {
		Enumeration<JSVTreeNode> enume = rootNode.children();
    while (enume.hasMoreElements()) {
      JSVTreeNode treeNode = enume.nextElement();
    	JCheckBox cb = new JCheckBox();
    	JSVSpecNode node = treeNode.specNode;
    	cb.setSelected(node.isSelected);
    	cb.setText(node.toString());
    	cb.setActionCommand("" + (treeNodes.size()));
      cb.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(ActionEvent e) {
          check(e);
        }
      });
      Insets insets = (level < 1 ? cbInsets1 : cbInsets2);
      spectrumPanel.add(cb, new GridBagConstraints(0, checkBoxes.size(), 1, 1, 0.0, 0.0,
      		GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
      treeNode.index = treeNodes.size();
    	treeNodes.add(treeNode);
    	checkBoxes.add(cb);
    	addCheckBoxes(treeNode, level + 1);
    }
	}

	private void checkEnables() {		
		closeSelectedButton.setEnabled(false);
		for (int i = 0; i < checkBoxes.size(); i++) {
			if (checkBoxes.get(i).isSelected() && treeNodes.get(i).specNode.jsvp == null) {
				closeSelectedButton.setEnabled(true);
				break;
			}
		}
		
		overlaySelectedButton.setEnabled(false);
		int n = 0;
		for (int i = 0; i < checkBoxes.size(); i++) {
			if (checkBoxes.get(i).isSelected() && treeNodes.get(i).specNode.jsvp != null) {
				n++;
			}
		}
		overlaySelectedButton.setEnabled(n > 0);
		overlaySelectedButton.setText(n == 1 ? "View Selected" : "Overlay Selected");
	}
	
	private boolean checking = false; 
	
	@SuppressWarnings("unchecked")
	protected void check(ActionEvent e) {
		int i = Integer.parseInt(e.getActionCommand());
		JSVTreeNode node = treeNodes.get(i);
		JCheckBox cb = (JCheckBox) e.getSource();
		boolean isSelected = cb.isSelected();
		if (node.specNode.jsvp == null) {
			if (!checking && isSelected && cb.getText().startsWith("Overlay")) {
				checking = true;
				select(false);
				cb.setSelected(true);
				node.specNode.isSelected = true;
				checking = false;
			}
			Enumeration<JSVTreeNode> enume = node.children();
			while (enume.hasMoreElements()) {
				JSVTreeNode treeNode = enume.nextElement();
				checkBoxes.get(treeNode.index).setSelected(isSelected);
				treeNode.specNode.isSelected = isSelected;
			}
		} else {
			// uncheck all Overlays
			node.specNode.isSelected = isSelected;
		}
		if (isSelected)
			for (i = treeNodes.size(); --i >= 0;)
				if (treeNodes.get(i).specNode.isOverlay != node.specNode.isOverlay)
					checkBoxes.get(treeNodes.get(i).index).setSelected(false);
		checkEnables();
	}

	protected void select(boolean mode) {
		for (int i = checkBoxes.size(); --i >= 0;) {
			checkBoxes.get(i).setSelected(mode);
			treeNodes.get(i).specNode.isSelected = mode;
		}
		checkEnables();
	}
	
	protected void overlaySelected() {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < checkBoxes.size(); i++) {
			JCheckBox cb = checkBoxes.get(i); 
			if (cb.isSelected() && treeNodes.get(i).specNode.jsvp != null 
					&& !((JSVTreeNode)treeNodes.get(i).getParent()).specNode.fileName.startsWith("Overlay")) {
				String label = cb.getText();
				sb.append(" ").append(label.substring(0, label.indexOf(":")));
			}
		}
		JSViewer.execOverlay(si, sb.toString().trim(), false);
		setup();
	}

	protected void closeSelected() {
		si.execClose("selected", false);
    setup();
	}

  protected void done() {
  	dispose();
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
		dispose();
	}

	public void windowDeactivated(WindowEvent arg0) {
		dispose();
	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent arg0) {
		dispose();
	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

}

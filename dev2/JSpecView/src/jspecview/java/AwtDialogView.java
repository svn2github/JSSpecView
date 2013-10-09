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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Enumeration;


import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import jspecview.api.JSVTreeNode;
import jspecview.api.ScriptInterface;
import jspecview.common.JSVPanelNode;
import jspecview.common.JSViewer;

import org.jmol.util.JmolList;
import org.jmol.util.SB;



/**
 * Dialog for managing overlaying spectra and closing files
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 */
public class AwtDialogView extends AwtDialog implements WindowListener {

	private static final long serialVersionUID = 1L;
	private ScriptInterface si;
	private JmolList<JSVTreeNode> treeNodes;
	
	
	private JmolList<JCheckBox> checkBoxes;
	private JPanel spectrumPanel;
	private Insets cbInsets1;
	private Insets cbInsets2;
	private JButton closeSelectedButton;
	private JButton combineSelectedButton;
	private JButton viewSelectedButton;
	private JSViewer viewer;
  
	private static int[] posXY = new int[] {Integer.MIN_VALUE, 0};
  
	/**
	 * Initialises the <code>IntegralDialog</code> with the given values for minY,
	 * offset and factor
	 * @param viewer 
	 * 
	 * @param panel
	 *          the parent panel
	 * @param modal
	 *          the modality
	 */
	public AwtDialogView(JSViewer viewer, Component panel, boolean modal) {
		super(null, "View/Combine/Close Spectra", modal);
		this.viewer = viewer;
		restoreDialogPosition(panel, posXY);
		setResizable(true);
		addWindowListener(this);
		setup();
	}

	private void setup() {
    try {
      jbInit();
      pack();
      setVisible(true);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    //bounds = getBounds();
	}

  void jbInit() throws Exception {
    layoutCheckBoxes();
        
    JButton selectAllButton = newJButton();
    JButton selectNoneButton = newJButton();
    combineSelectedButton = newJButton();
    viewSelectedButton = newJButton();
    closeSelectedButton = newJButton();
    JButton doneButton = newJButton();

    selectAllButton.setText("Select All");
    selectAllButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        select(true);
      }
    });
    
    selectNoneButton.setText("Select None");
    selectNoneButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        select(false);
      }
    });
    
    viewSelectedButton.setText("View Selected");
    viewSelectedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        viewSelected();
      }
    });
    
    combineSelectedButton.setText("Combine Selected");
    combineSelectedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        combineSelected();
      }
    });
    
    closeSelectedButton.setText("Close Selected");
    closeSelectedButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        closeSelected();
      }
    });
    
    doneButton.setText("Done");
    doneButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        done();
      }
    });

    Insets buttonInsets = new Insets(5, 5, 5, 5);
    JPanel leftPanel = new JPanel(new GridBagLayout());
    leftPanel.setMinimumSize(new Dimension(150, 300));
    int i = 0;
    addButton(leftPanel, selectAllButton, i++, buttonInsets);
    addButton(leftPanel, selectNoneButton, i++, buttonInsets);
    addButton(leftPanel, viewSelectedButton, i++, buttonInsets);
    addButton(leftPanel, combineSelectedButton, i++, buttonInsets);
    addButton(leftPanel, closeSelectedButton, i++, buttonInsets);
    addButton(leftPanel, doneButton, i++, buttonInsets);
        
    JScrollPane scrollPane = new JScrollPane(spectrumPanel);

    JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    mainSplitPane.setOneTouchExpandable(true);
    mainSplitPane.setResizeWeight(0);
    mainSplitPane.setRightComponent(scrollPane);
    mainSplitPane.setLeftComponent(leftPanel);

    setPreferredSize(new Dimension(500,350));
    getContentPane().removeAll();
    getContentPane().add(mainSplitPane);//, BorderLayout.CENTER);


    //getContentPane().add(mainPanel);
    checkEnables();
  }

	private void addButton(JPanel leftPanel, JButton selectAllButton, int i,
			Insets buttonInsets) {
    leftPanel.add(selectAllButton, new GridBagConstraints(0, i, 1, 1, 0.0, 0.0,
    		GridBagConstraints.CENTER, GridBagConstraints.NONE, buttonInsets, 0, 0));    
	}

	private JButton newJButton() {
		JButton b = new JButton();
		b.setPreferredSize(new Dimension(120,25));
		return b;
	}

	private void layoutCheckBoxes() {
    checkBoxes = new JmolList<JCheckBox>();
    treeNodes = new JmolList<JSVTreeNode>();
    cbInsets1 = new Insets(0, 0, 2, 2);
    cbInsets2 = new Insets(0, 20, 2, 2);
		spectrumPanel = new JPanel(new GridBagLayout());
    addCheckBoxes(((AwtTree) viewer.spectraTree).getRootNode(), 0, true);
    addCheckBoxes(((AwtTree) viewer.spectraTree).getRootNode(), 0, false);
	}

	private void addCheckBoxes(JSVTreeNode rootNode, int level, boolean addViews) {
		Enumeration<JSVTreeNode> enume = rootNode.children();
    while (enume.hasMoreElements()) {
      JSVTreeNode treeNode = enume.nextElement();
    	JSVPanelNode node = treeNode.getPanelNode();
    	if (node.isView != addViews)
    		continue;
    	JCheckBox cb = new JCheckBox();
    	cb.setSelected(node.isSelected);
    	String title = node.toString();
    	if (title.indexOf("\n") >= 0)
    		title = title.substring(0, title.indexOf('\n'));
    	cb.setText(title);
    	cb.setActionCommand("" + (treeNodes.size()));
      cb.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          check(e);
        }
      });
      Insets insets = (level < 1 ? cbInsets1 : cbInsets2);
      spectrumPanel.add(cb, new GridBagConstraints(0, checkBoxes.size(), 1, 1, 0.0, 0.0,
      		GridBagConstraints.WEST, GridBagConstraints.NONE, insets, 0, 0));
      treeNode.setIndex(treeNodes.size());
    	treeNodes.addLast(treeNode);
    	checkBoxes.addLast(cb);
    	addCheckBoxes(treeNode, level + 1, addViews);
    }
	}

	private void checkEnables() {		
		closeSelectedButton.setEnabled(false);
		for (int i = 0; i < checkBoxes.size(); i++) {
			if (checkBoxes.get(i).isSelected() && treeNodes.get(i).getPanelNode().jsvp == null) {
				closeSelectedButton.setEnabled(true);
				break;
			}
		}
		
		int n = 0;
		for (int i = 0; i < checkBoxes.size(); i++) {
			if (checkBoxes.get(i).isSelected() && treeNodes.get(i).getPanelNode().jsvp != null) {
				n++;
			}
		}
		combineSelectedButton.setEnabled(n > 1);
		viewSelectedButton.setEnabled(n == 1);
	}
	
	private boolean checking = false; 
	
	protected void check(ActionEvent e) {
		int i = Integer.parseInt(e.getActionCommand());
		JSVTreeNode node = treeNodes.get(i);
		JCheckBox cb = (JCheckBox) e.getSource();
		boolean isSelected = cb.isSelected();
		if (node.getPanelNode().jsvp == null) {
			if (!checking && isSelected && cb.getText().startsWith("Overlay")) {
				checking = true;
				select(false);
				cb.setSelected(true);
				node.getPanelNode().isSelected = true;
				checking = false;
			}
			Enumeration<JSVTreeNode> enume = node.children();
			while (enume.hasMoreElements()) {
				JSVTreeNode treeNode = enume.nextElement();
				checkBoxes.get(treeNode.getIndex()).setSelected(isSelected);
				treeNode.getPanelNode().isSelected = isSelected;
				node.getPanelNode().isSelected = isSelected;
			}
		} else {
			// uncheck all Overlays
			node.getPanelNode().isSelected = isSelected;
		}
		if (isSelected)
			for (i = treeNodes.size(); --i >= 0;)
				if (treeNodes.get(i).getPanelNode().isView != node.getPanelNode().isView) {
					checkBoxes.get(treeNodes.get(i).getIndex()).setSelected(false);
					treeNodes.get(i).getPanelNode().isSelected = false;
				}
		checkEnables();
	}

	protected void select(boolean mode) {
		for (int i = checkBoxes.size(); --i >= 0;) {
			checkBoxes.get(i).setSelected(mode);
			treeNodes.get(i).getPanelNode().isSelected = mode;
		}
		checkEnables();
	}
	
	protected void combineSelected() {
		SB sb = new SB();
		for (int i = 0; i < checkBoxes.size(); i++) {
			JCheckBox cb = checkBoxes.get(i);
			JSVPanelNode node = treeNodes.get(i).getPanelNode();
			if (cb.isSelected() && node.jsvp != null) {
				if (node.isView) {
					si.siSetNode(node, true);
					return;
				}
				String label = cb.getText();
				sb.append(" ").append(label.substring(0, label.indexOf(":")));
			}
		}
		viewer.execView(sb.toString().trim(), false);
		setup();
	}

	protected void viewSelected() {
		SB sb = new SB();
		for (int i = 0; i < checkBoxes.size(); i++) {
			JCheckBox cb = checkBoxes.get(i);
			JSVPanelNode node = treeNodes.get(i).getPanelNode();
			if (cb.isSelected() && node.jsvp != null) {
				if (node.isView) {
					si.siSetNode(node, true);
					return;
				}
				String label = cb.getText();
				sb.append(" ").append(label.substring(0, label.indexOf(":")));
			}
		}
		viewer.execView(sb.toString().trim(), false);
		setup();
	}

	protected void closeSelected() {
		si.siExecClose("selected", false);
    setup();
	}

  protected void done() {
  	saveDialogPosition(posXY);
  	dispose();
	}

	public void windowActivated(WindowEvent arg0) {
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
		done();
	}

	public void windowDeactivated(WindowEvent arg0) {
	}

	public void windowDeiconified(WindowEvent arg0) {
	}

	public void windowIconified(WindowEvent arg0) {
	}

	public void windowOpened(WindowEvent arg0) {
	}

}

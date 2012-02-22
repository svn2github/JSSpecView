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
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Dialog for inputing values for the calulation of the <code>IntegralGraph</code>.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class IntegrateDialog extends JDialog {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  // Gui components
  private JPanel jPanel4 = new JPanel();
  private GridBagLayout gridBagLayout1 = new GridBagLayout();
  private JLabel minYLabel = new JLabel();
  private JLabel factorLabel = new JLabel();
  private JLabel offsetLabel = new JLabel();
  private JTextField offsetTextField = new JTextField();
  private JTextField factorTextField = new JTextField();
  private JTextField minYTextField = new JTextField();
  private JLabel percentLabel1 = new JLabel();
  private JLabel percentLabel2 = new JLabel();
  private JLabel jpercentLabel3 = new JLabel();
  private JButton okButton = new JButton();

  /**
   * The percentage minimum Y value
   */
  private double minY;

  /**
   * The percentage offset value
   */
  private double offset;

  /**
   * The percentage factor
   */
  private double factor;

  /**
    * Initialses the <code>IntegralDialog with the given values for minY, offset
    * and factor
    * @param panel the parent panel
    * @param title the title of the dialog
    * @param modal the modality
    * @param minY the minimum percent Y
    * @param offset the offset
    * @param factor the integral factor
   */
  public IntegrateDialog(JPanel panel, String title, boolean modal, double minY, double offset, double factor) {
    this.minY = minY;
    this.offset = offset;
    this.factor = factor;

    this.setTitle(title);
    this.setModal(modal);

    try {
      jbInit();
      setResizable(false);

      // Sets the location to the middle of the parent frame if it has one
      if(panel != null)
        setLocation( panel.getLocationOnScreen().x + (panel.getSize().width / 2),
                     panel.getLocationOnScreen().y + (panel.getSize().height / 2));

      pack();
      setVisible(true);

    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  void jbInit() throws Exception {
    jPanel4.setLayout(gridBagLayout1);
    minYLabel.setText("Minimum Y");
    factorLabel.setToolTipText("");
    factorLabel.setText("Integral Factor");
    offsetLabel.setText("Integral Offset");
    offsetTextField.setMinimumSize(new Dimension(40, 21));
    offsetTextField.setPreferredSize(new Dimension(40, 21));
    offsetTextField.setText(String.valueOf(offset));
    offsetTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    factorTextField.setMinimumSize(new Dimension(40, 21));
    factorTextField.setPreferredSize(new Dimension(40, 21));
    factorTextField.setText(String.valueOf(factor));
    factorTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    percentLabel1.setText("%");
    percentLabel2.setText("%");
    jpercentLabel3.setText("%");
    minYTextField.setMinimumSize(new Dimension(40, 21));
    minYTextField.setPreferredSize(new Dimension(40, 21));
    minYTextField.setToolTipText("");
    minYTextField.setText(String.valueOf(minY));
    minYTextField.setHorizontalAlignment(SwingConstants.RIGHT);
    okButton.setText("OK");
    okButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        okButton_actionPerformed(e);
      }
    });
    this.getContentPane().add(jPanel4,  BorderLayout.CENTER);
    jPanel4.add(factorLabel,         new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 50, 10));
    jPanel4.add(minYLabel,               new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 50, 10));
    jPanel4.add(offsetLabel,      new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 50, 10));
    jPanel4.add(offsetTextField, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel4.add(factorTextField,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel4.add(minYTextField,    new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel4.add(percentLabel1,      new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 2, 0, 0), 5, 0));
    jPanel4.add(percentLabel2,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel4.add(jpercentLabel3,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    jPanel4.add(okButton,    new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0
            ,GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
  }

  /**
   * Returns the minimum percent Y value
   * @return the minimum percent Y value
   */
  public double getMinimumY(){
    return minY;
  }

  /**
   * Returns the integral factor
   * @return the integral factor
   */
  public double getFactor(){
    return factor;
  }

  /**
   * Returns the integral offset value
   * @return the integral offset value
   */
  public double getOffset(){
    return offset;
  }

  /**
   * Called when the ok button of the dialog is pressed. Get the values that
   * have been entered and stores them as variables
   * @param e the <code>ActionEvent</code>
   */
  void okButton_actionPerformed(ActionEvent e) {
    try {
      minY = Double.parseDouble(minYTextField.getText());
      factor = Double.parseDouble(factorTextField.getText());
      offset = Double.parseDouble(offsetTextField.getText());
    }
    catch (NumberFormatException ex) {
    }

    dispose();
  }
}

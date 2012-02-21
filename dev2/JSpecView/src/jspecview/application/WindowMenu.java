/* Copyright (C) 2002-2012  The University of the West Indies
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

package jspecview.application;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Menu component that handles the functionality expected of a standard
 * "Windows" menu for MDI applications. Taken from www.javaworld.com (originally
 * mditest package)
 * 
 * @author http://www.javaworld.com
 */
public class WindowMenu extends JMenu {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private ScrollableDesktopPane desktop;
  private JCheckBoxMenuItem stack = new JCheckBoxMenuItem("Stack");
  private JCheckBoxMenuItem cascade = new JCheckBoxMenuItem("Cascade");
  private JCheckBoxMenuItem tile = new JCheckBoxMenuItem("Tile");
  
  public void setMyStyle(int style) {
    stack.setSelected(style == ScrollableDesktopPane.STYLE_STACK);
    cascade.setSelected(style == ScrollableDesktopPane.STYLE_CASCADE);
    tile.setSelected(style == ScrollableDesktopPane.STYLE_TILE);
  }

  public WindowMenu(ScrollableDesktopPane desktop) {
    desktop.setWindowMenu(this);
    this.desktop = desktop;
    setText("Window");
    add(stack);
    stack.setSelected(true);
    add(cascade);
    add(tile);

    addSeparator();
    stack.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        WindowMenu.this.desktop.stackFrames();
      }
    });
    cascade.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        WindowMenu.this.desktop.cascadeFrames();
      }
    });
    tile.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        WindowMenu.this.desktop.tileFrames();
      }
    });
    addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent e) {
      }

      public void menuDeselected(MenuEvent e) {
        Component[] menuItems = getMenuComponents();
        for (int i = 3; i < menuItems.length; i++) {
          if (menuItems[i] instanceof JCheckBoxMenuItem) {
            remove(menuItems[i]);
          }
        }
      }

      public void menuSelected(MenuEvent e) {
        buildChildMenus();
      }
    });
  }

  /* Sets up the children menus depending on the current desktop state */
  private void buildChildMenus() {
    int i;
    ChildMenuItem menu;
    JInternalFrame[] array = desktop.getAllFrames();

    stack.setEnabled(array.length > 0);
    cascade.setEnabled(array.length > 0);
    tile.setEnabled(array.length > 0);

    for (i = 0; i < array.length; i++) {
      menu = new ChildMenuItem(array[i]);
      menu.setState(i == 0);
      menu.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ae) {
          JInternalFrame frame = ((ChildMenuItem) ae.getSource()).getFrame();
          frame.moveToFront();
          try {
            frame.setSelected(true);
          } catch (PropertyVetoException e) {
            e.printStackTrace();
          }
        }
      });
      menu.setIcon(array[i].getFrameIcon());
      add(menu);
    }
  }

  /* This JCheckBoxMenuItem descendant is used to track the child frame that corresponds
     to a give menu. */
  class ChildMenuItem extends JCheckBoxMenuItem {
    /**
       * 
       */
    private static final long serialVersionUID = 1L;
    private JInternalFrame frame;

    public ChildMenuItem(JInternalFrame frame) {
      super(frame.getTitle());
      this.frame = frame;
    }

    public JInternalFrame getFrame() {
      return frame;
    }
  }
}

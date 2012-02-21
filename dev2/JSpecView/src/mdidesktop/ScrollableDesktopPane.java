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

package mdidesktop;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;


/**
 * An extension of JDesktopPane that supports often used MDI functionality. This
 * class also handles setting scroll bars for when windows move too far to the left or
 * bottom, providing the ScrollableDesktopPane is in a ScrollPane.
 * Taken from www.javaworld.com (originally mditest package)
 * @author http://www.javaworld.com
 */
public class ScrollableDesktopPane extends JDesktopPane {

  private static final long serialVersionUID = 1L;
    private static int FRAME_OFFSET=20;
    private MDIDesktopManager manager;

    public ScrollableDesktopPane() {
        manager=new MDIDesktopManager(this);
        setDesktopManager(manager);
        setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);
    }

    public void setBounds(int x, int y, int w, int h) {
        super.setBounds(x,y,w,h);
        checkDesktopSize();
    }

    public Component add(JInternalFrame frame) {
        //JInternalFrame[] array = getAllFrames();
        //Point p;
        int w;
        int h;

        Component retval=super.add(frame);
        checkDesktopSize();
        /*
        if (array.length > 0) {
            p = array[0].getLocation();
            p.x = p.x + FRAME_OFFSET;
            p.y = p.y + FRAME_OFFSET;
        }
        else {
            p = new Point(0, 0);
        }
        frame.setLocation(p.x, p.y);
        */
        if (frame.isResizable()) {
            w = getWidth() - (getWidth()/3);
            h = getHeight() - (getHeight()/3);
            if (w < frame.getMinimumSize().getWidth()) w = (int)frame.getMinimumSize().getWidth();
            if (h < frame.getMinimumSize().getHeight()) h = (int)frame.getMinimumSize().getHeight();
            frame.setSize(w, h);
        }
        moveToFront(frame);
        frame.setVisible(true);
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            frame.toBack();
        }
        return retval;
    }

    public void remove(Component c) {
        super.remove(c);
        checkDesktopSize();
    }

  public void stackFrames() {
    manager.setNormalSize();
    JInternalFrame allFrames[] = getAllFrames();
    for (int i = allFrames.length - 1; i >= 0; i--) {
      try {
        allFrames[i].setEnabled(false);
        allFrames[i].setMaximum(false); // also activates?
        allFrames[i].setMaximum(true);
        allFrames[i].setEnabled(true);
      } catch (PropertyVetoException e) {
      }
    }    
  }

    /**
     * Cascade all internal frames
     */
    public void cascadeFrames() {
        int x = 0;
        int y = 0;
        JInternalFrame allFrames[] = getAllFrames();

        manager.setNormalSize();
        int frameHeight = (getBounds().height - 5) - allFrames.length * FRAME_OFFSET;
        int frameWidth = (getBounds().width - 5) - allFrames.length * FRAME_OFFSET;
        for (int i = allFrames.length - 1; i >= 0; i--) {
            allFrames[i].setSize(frameWidth,frameHeight);
            allFrames[i].setLocation(x,y);
            x = x + FRAME_OFFSET;
            y = y + FRAME_OFFSET;
        }
    }

    /**
     * Tile all internal frames
     */
    public void tileFrames() {
      JInternalFrame[] frames = getAllFrames();
      manager.setNormalSize();

      JScrollPane scrollPane = manager.getScrollPane();
      Rectangle viewP;
      if(scrollPane != null)
        viewP = scrollPane.getViewport().getViewRect();
      else
        viewP = getBounds();

      int totalNonIconFrames=0;

      for (int i=0; i < frames.length; i++) {
        if (!frames[i].isIcon()) {    // don't include iconified frames...
          totalNonIconFrames++;
        }
      }

      int curCol = 0;
      int curRow = 0;
      int i=0;

      if (totalNonIconFrames > 0) {
        // compute number of columns and rows then tile the frames
        int numCols = (int)Math.sqrt(totalNonIconFrames);

        int frameWidth = viewP.width/numCols;

        for (curCol=0; curCol < numCols; curCol++) {
          int numRows = totalNonIconFrames / numCols;
          int remainder = totalNonIconFrames % numCols;

          if ((numCols-curCol) <= remainder) {
                numRows++; // add an extra row for this guy
          }

          int frameHeight = viewP.height/numRows;

          for (curRow=0; curRow < numRows; curRow++) {
            while (frames[i].isIcon()) { // find the next visible frame
                  i++;
            }

            frames[i].setBounds(curCol*frameWidth,curRow*frameHeight,
                  frameWidth,frameHeight);

            i++;
          }
        }
      }
      
      
    }

    /*
     * Sets all component size properties ( maximum, minimum, preferred)
     * to the given dimension.
     */
    public void setAllSize(Dimension d){
        setMinimumSize(d);
        setMaximumSize(d);
        setPreferredSize(d);
    }

    /*
     * Sets all component size properties ( maximum, minimum, preferred)
     * to the given width and height.
     */
    public void setAllSize(int width, int height){
        setAllSize(new Dimension(width,height));
    }

    private void checkDesktopSize() {
        if (getParent()!=null&&isVisible()) manager.resizeDesktop();
    }
}

/**
 * Private class used to replace the standard DesktopManager for JDesktopPane.
 * Used to provide scrollbar functionality.
 */
class MDIDesktopManager extends DefaultDesktopManager {
    /**
   * 
   */
  private static final long serialVersionUID = 1L;
    private ScrollableDesktopPane desktop;

    public MDIDesktopManager(ScrollableDesktopPane desktop) {
        this.desktop = desktop;
    }

    public void endResizingFrame(JComponent f) {
        super.endResizingFrame(f);
        resizeDesktop();
    }

    public void endDraggingFrame(JComponent f) {
        super.endDraggingFrame(f);
        resizeDesktop();
    }

    public void setNormalSize() {
        JScrollPane scrollPane=getScrollPane();
        int x = 0;
        int y = 0;
        Insets scrollInsets = getScrollPaneInsets();

        if (scrollPane != null) {
            Dimension d = scrollPane.getVisibleRect().getSize();
            if (scrollPane.getBorder() != null) {
               d.setSize(d.getWidth() - scrollInsets.left - scrollInsets.right,
                         d.getHeight() - scrollInsets.top - scrollInsets.bottom);
            }

            d.setSize(d.getWidth() - 20, d.getHeight() - 20);
            x = (int) d.getWidth();
            y = (int) d.getHeight();
            desktop.setAllSize(x,y);
            scrollPane.invalidate();
            scrollPane.validate();
        }
    }

    private Insets getScrollPaneInsets() {
        JScrollPane scrollPane=getScrollPane();
        if (scrollPane==null) return new Insets(0,0,0,0);
        else return getScrollPane().getBorder().getBorderInsets(scrollPane);
    }

    public JScrollPane getScrollPane() {
        if (desktop.getParent() instanceof JViewport) {
            JViewport viewPort = (JViewport)desktop.getParent();
            if (viewPort.getParent() instanceof JScrollPane)
                return (JScrollPane)viewPort.getParent();
        }
        return null;
    }

  protected void resizeDesktop() {
    int x = 0;
    int y = 0;
    JScrollPane scrollPane = getScrollPane();
    Insets scrollInsets = getScrollPaneInsets();

    if (scrollPane != null) {
      Dimension d = scrollPane.getVisibleRect().getSize();
      JInternalFrame allFrames[] = desktop.getAllFrames();
      for (int i = 0; i < allFrames.length; i++) {
        if (allFrames[i].getX() + allFrames[i].getWidth() > x) {
          x = allFrames[i].getX() + allFrames[i].getWidth();
        }
        if (allFrames[i].getY() + allFrames[i].getHeight() > y) {
          y = allFrames[i].getY() + allFrames[i].getHeight();
        }
      }
      if (scrollPane.getBorder() != null) {
        d.setSize(d.getWidth() - scrollInsets.left - scrollInsets.right, d
            .getHeight()
            - scrollInsets.top - scrollInsets.bottom);
      }

      if (x != d.getWidth())
        x = ((int) d.getWidth()) - 20;
      if (y != d.getHeight())
        y = ((int) d.getHeight()) - 20;
      desktop.setAllSize(x, y);
      scrollPane.invalidate();
      scrollPane.validate();
    }
  }
    

}

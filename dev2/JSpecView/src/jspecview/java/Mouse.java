/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2013-09-25 15:33:17 -0500 (Wed, 25 Sep 2013) $
 * $Revision: 18695 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package jspecview.java;

import java.awt.Component;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javajs.api.GenericMouseInterface;
import javajs.awt.event.Event;
import javajs.api.EventManager;

import jspecview.api.JSVPanel;

import org.jmol.util.Logger;

/**
 * formerly org.jmol.viewer.MouseManager14
 * 
 * methods required by Jmol that access java.awt.event
 * 
 * private to jspecview.java
 * 
 */

class Mouse implements MouseWheelListener, MouseListener,
    MouseMotionListener, KeyListener, GenericMouseInterface {

  private JSVPanel viewer;
  private EventManager pd;

  /**
   * @param jsvp 
   */
  Mouse(JSVPanel jsvp) {
  	this.viewer = jsvp;
    pd = jsvp.getPanelData();
    Component display = (Component) jsvp;
    display.addKeyListener(this);
    display.addMouseListener(this);
    display.addMouseMotionListener(this);
    display.addMouseWheelListener(this);
  }

  @Override
	public void clear() {
    // nothing to do here now -- see ActionManager
  }

  @Override
	public void dispose() {
    Component display = (Component) viewer;
    display.removeMouseListener(this);
    display.removeMouseMotionListener(this);
    display.removeMouseWheelListener(this);
    display.removeKeyListener(this);
  }

  @Override
	public boolean processEvent(int id, int x, int y, int modifiers, long time) {
    if (pd == null) {
      if (id == Event.MOUSE_DOWN) {
        viewer.showMenu(x, y);        
      }      
      return true;
    }
    modifiers = applyLeftMouse(modifiers);
    switch (id) {
    case Event.MOUSE_DOWN:
      xWhenPressed = x;
      yWhenPressed = y;
      modifiersWhenPressed10 = modifiers;
      mousePressed(time, x, y, modifiers);
      break;
    case Event.MOUSE_DRAG:
      mouseDragged(time, x, y, modifiers);
      break;
    case Event.MOUSE_ENTER:
      mouseEntered(time, x, y);
      break;
    case Event.MOUSE_EXIT:
      mouseExited(time, x, y);
      break;
    case Event.MOUSE_MOVE:
      mouseMoved(time, x, y, modifiers);
      break;
    case Event.MOUSE_UP:
      mouseReleased(time, x, y, modifiers);
      // simulate a mouseClicked event for us
      if (x == xWhenPressed && y == yWhenPressed
          && modifiers == modifiersWhenPressed10) {
        // the underlying code will turn this into dbl clicks for us
        mouseClicked(time, x, y, modifiers, 1);
      }
      break;
    default:
      return false;
    }
    return true;
  }

  @Override
	public void mouseClicked(MouseEvent e) {
    if (pd != null)
      mouseClicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .getClickCount());
  }

  @Override
	public void mouseEntered(MouseEvent e) {
    if (pd != null)
      mouseEntered(e.getWhen(), e.getX(), e.getY());
  }

  @Override
	public void mouseExited(MouseEvent e) {
    if (pd != null)
    	mouseExited(e.getWhen(), e.getX(), e.getY());
  }

  @Override
	public void mousePressed(MouseEvent e) {
    if (pd == null) {
      viewer.showMenu(e.getX(), e.getY());        
      return;
    }
    mousePressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  @Override
	public void mouseReleased(MouseEvent e) {
    if (pd != null)
    	mouseReleased(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  @Override
	public void mouseDragged(MouseEvent e) {
    if (pd == null)
      return;
    int modifiers = e.getModifiers();
    /****************************************************************
     * Netscape 4.* Win32 has a problem with mouseDragged if you left-drag then
     * none of the modifiers are selected we will try to fix that here
     ****************************************************************/
    if ((modifiers & Event.BUTTON_MASK) == 0)
      modifiers |= Event.MOUSE_LEFT;
    
    /****************************************************************/
    mouseDragged(e.getWhen(), e.getX(), e.getY(), modifiers);
  }

  @Override
	public void mouseMoved(MouseEvent e) {
    if (pd != null)
    	mouseMoved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  @Override
	public void mouseWheelMoved(MouseWheelEvent e) {
    if (pd == null)
      return;
    e.consume();
    mouseWheel(e.getWhen(), e.getWheelRotation(), e.getModifiers()
        | Event.MOUSE_WHEEL);
  }

  @Override
	public void keyTyped(KeyEvent ke) {
    if (pd == null)
      return;
    char ch = ke.getKeyChar();
    int modifiers = ke.getModifiers();
    // for whatever reason, CTRL may also drop the 6- and 7-bits,
    // so we are in the ASCII non-printable region 1-31
    if (Logger.debuggingHigh)
      Logger.info("MouseManager keyTyped: " + ch + " " + (0+ch) + " " + modifiers);
    if (pd.keyTyped(ch, modifiers))
  		ke.consume();
  }

  @Override
	public void keyPressed(KeyEvent ke) {
    if (pd != null && pd.keyPressed(ke.getKeyCode(), ke.getModifiers()))
    	ke.consume();
  }

  @Override
	public void keyReleased(KeyEvent ke) {
    if (pd != null)
      pd.keyReleased(ke.getKeyCode());
  }

  private void mouseEntered(long time, int x, int y) {
    pd.mouseEnterExit(time, x, y, false);
  }

  private void mouseExited(long time, int x, int y) {
    pd.mouseEnterExit(time, x, y, true);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param clickCount
   */
  private void mouseClicked(long time, int x, int y, int modifiers, int clickCount) {
    // clickedCount is not reliable on some platforms
    // so we will just deal with it ourselves
    pd.mouseAction(Event.CLICKED, time, x, y, 1, modifiers);
  }

  private boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT as drag, only move
  
  private void mouseMoved(long time, int x, int y, int modifiers) {
    if (isMouseDown)
      pd.mouseAction(Event.DRAGGED, time, x, y, 0, applyLeftMouse(modifiers));
    else
      pd.mouseAction(Event.MOVED, time, x, y, 0, modifiers);
  }

  private void mouseWheel(long time, int rotation, int modifiers) {
    pd.mouseAction(Event.WHEELED, time, 0, rotation, 0, modifiers);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   */
  private void mousePressed(long time, int x, int y, int modifiers) {
    isMouseDown = true;
    pd.mouseAction(Event.PRESSED, time, x, y, 0, modifiers);
  }

  private void mouseReleased(long time, int x, int y, int modifiers) {
    isMouseDown = false;
    pd.mouseAction(Event.RELEASED, time, x, y, 0, modifiers);
  }

  private void mouseDragged(long time, int x, int y, int modifiers) {
    if ((modifiers & Event.MAC_COMMAND) == Event.MAC_COMMAND)
      modifiers = modifiers & ~Event.MOUSE_RIGHT | Event.CTRL_MASK; 
    pd.mouseAction(Event.DRAGGED, time, x, y, 0, modifiers);
  }

  private static int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & Event.BUTTON_MASK) == 0) ? (modifiers | Event.MOUSE_LEFT)
        : modifiers;
  }

  private int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

	@Override
	public void processTwoPointGesture(float[][][] touches) {
		// TODO Auto-generated method stub
		
	}

}

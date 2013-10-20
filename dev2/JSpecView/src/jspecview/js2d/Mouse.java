/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2012-09-11 19:29:26 -0500 (Tue, 11 Sep 2012) $
 * $Revision: 17556 $
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
package jspecview.js2d;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javajs.J2SRequireImport;

import jspecview.api.JSVPanel;

import org.jmol.api.EventManager;
import org.jmol.api.JmolMouseInterface;
import org.jmol.api.Event;
import org.jmol.util.Logger;

/**
 * JavaScript interface from JmolJSmol.js via handleOldJvm10Event (for now)
 * 
 * J2SRequireImport is needed because we want to allow JavaScript access to java.awt.Event constant names
 * 
 */

@J2SRequireImport({org.jmol.api.Event.class})
public class Mouse implements JmolMouseInterface {

  private EventManager pd;

  public Mouse(JSVPanel viewer) {
  	pd = viewer.getPanelData();
  }

  public void clear() {
    // nothing to do here now -- see ActionManager
  }

  public void dispose() {
    // nothing to do here
  }

  private int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

  public boolean handleOldJvm10Event(int id, int x, int y, int modifiers, long time) {
    if (id != -1)
      modifiers = applyLeftMouse(modifiers);
    switch (id) {
    case -1: // JavaScript
      wheeled(time, x, modifiers | Event.MOUSE_WHEEL);
      break;
    case Event.MOUSE_DOWN:
      xWhenPressed = x;
      yWhenPressed = y;
      modifiersWhenPressed10 = modifiers;
      pressed(time, x, y, modifiers, false);
      break;
    case Event.MOUSE_DRAG:
      dragged(time, x, y, modifiers);
      break;
    case Event.MOUSE_ENTER:
      entered(time, x, y);
      break;
    case Event.MOUSE_EXIT:
      exited(time, x, y);
      break;
    case Event.MOUSE_MOVE:
      moved(time, x, y, modifiers);
      break;
    case Event.MOUSE_UP:
      released(time, x, y, modifiers);
      // simulate a mouseClicked event for us
      if (x == xWhenPressed && y == yWhenPressed
          && modifiers == modifiersWhenPressed10) {
        // the underlying code will turn this into dbl clicks for us
        clicked(time, x, y, modifiers, 1);
      }
      break;
    default:
      return false;
    }
    return true;
  }

  public void mouseClicked(MouseEvent e) {
    clicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .getClickCount());
  }

  public void mouseEntered(MouseEvent e) {
    entered(e.getWhen(), e.getX(), e.getY());
  }

  public void mouseExited(MouseEvent e) {
    exited(e.getWhen(), e.getX(), e.getY());
  }

  public void mousePressed(MouseEvent e) {
    pressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .isPopupTrigger());
  }

  public void mouseReleased(MouseEvent e) {
    released(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseDragged(MouseEvent e) {
    int modifiers = e.getModifiers();
    /****************************************************************
     * Netscape 4.* Win32 has a problem with mouseDragged if you left-drag then
     * none of the modifiers are selected we will try to fix that here
     ****************************************************************/
    if ((modifiers & Event.BUTTON_MASK) == 0)
      modifiers |= Event.MOUSE_LEFT;
    /****************************************************************/
    dragged(e.getWhen(), e.getX(), e.getY(), modifiers);
  }

  public void mouseMoved(MouseEvent e) {
    moved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    e.consume();
    wheeled(e.getWhen(), e.getWheelRotation(), e.getModifiers()
        | Event.MOUSE_WHEEL);
  }

  public void keyTyped(KeyEvent ke) {
    char ch = ke.getKeyChar();
    int modifiers = ke.getModifiers();
    // for whatever reason, CTRL may also drop the 6- and 7-bits,
    // so we are in the ASCII non-printable region 1-31
    if (Logger.debuggingHigh || true)
      Logger.info("MouseManager keyTyped: " + ch + " " + (0+ch) + " " + modifiers);
    if (pd.keyTyped(ch, modifiers))
  		ke.consume();
  }

  public void keyPressed(KeyEvent ke) {
    if (pd.keyPressed(ke.getKeyCode(), ke.getModifiers()))
    	ke.consume();
  }

  public void keyReleased(KeyEvent ke) {
    pd.keyReleased(ke.getKeyCode());
  }


  private void entered(long time, int x, int y) {
    pd.mouseEnterExit(time, x, y, false);
  }

  private void exited(long time, int x, int y) {
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
  private void clicked(long time, int x, int y, int modifiers, int clickCount) {
    // clickedCount is not reliable on some platforms
    // so we will just deal with it ourselves
    pd.mouseAction(Event.CLICKED, time, x, y, 1, modifiers);
  }

  private boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT as drag, only move
  
  private void moved(long time, int x, int y, int modifiers) {
    if (isMouseDown)
      pd.mouseAction(Event.DRAGGED, time, x, y, 0, applyLeftMouse(modifiers));
    else
      pd.mouseAction(Event.MOVED, time, x, y, 0, modifiers & ~Event.BUTTON_MASK);
  }

  private void wheeled(long time, int rotation, int modifiers) {
    pd.mouseAction(Event.WHEELED, time, 0, rotation, 0, modifiers);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param isPopupTrigger
   */
  private void pressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {
    isMouseDown = true;
    pd.mouseAction(Event.PRESSED, time, x, y, 0, modifiers);
  }

  private void released(long time, int x, int y, int modifiers) {
    isMouseDown = false;
    pd.mouseAction(Event.RELEASED, time, x, y, 0, modifiers);
  }

  private void dragged(long time, int x, int y, int modifiers) {
    if ((modifiers & Event.MAC_COMMAND) == Event.MAC_COMMAND)
      modifiers = modifiers & ~Event.MOUSE_RIGHT | Event.CTRL_MASK; 
    pd.mouseAction(Event.DRAGGED, time, x, y, 0, modifiers);
  }

  private static int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & Event.BUTTON_MASK) == 0) ? (modifiers | Event.MOUSE_LEFT)
        : modifiers;
  }

	public void processTwoPointGesture(float[][][] touches) {
		// TODO Auto-generated method stub
		
	}


}

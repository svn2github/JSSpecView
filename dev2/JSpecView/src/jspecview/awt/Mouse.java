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
package jspecview.awtjsv;

import java.awt.Component;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import jspecview.util.JSVEscape;

import org.jmol.api.JmolMouseInterface;
import org.jmol.api.Event;
import org.jmol.api.PlatformViewer;
import org.jmol.util.Logger;

/**
 * formerly org.jmol.viewer.MouseManager14
 * 
 * methods required by Jmol that access java.awt.event
 * 
 * private to jspecview.awt
 * 
 */

class Mouse implements MouseWheelListener, MouseListener,
    MouseMotionListener, KeyListener, JmolMouseInterface {

  //double privateKey;
  /**
   * @param privateKey  
   * @param viewer 
   */
  Mouse(double privateKey, PlatformViewer viewer) {
  }

  public void clear() {
    // nothing to do here now -- see ActionManager
  }

  public void dispose() {
//    Component display = (Component) viewer.getDisplay();
//    display.removeMouseListener(this);
//    display.removeMouseMotionListener(this);
//    display.removeMouseWheelListener(this);
//    display.removeKeyListener(this);
  }

  public boolean handleOldJvm10Event(int id, int x, int y, int modifiers, long time) {
    modifiers = applyLeftMouse(modifiers);
    switch (id) {
    case Event.MOUSE_DOWN:
      xWhenPressed = x;
      yWhenPressed = y;
      modifiersWhenPressed10 = modifiers;
      mousePressed(time, x, y, modifiers, false);
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

  public void mouseClicked(MouseEvent e) {
    mouseClicked(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .getClickCount());
  }

  public void mouseEntered(MouseEvent e) {
    mouseEntered(e.getWhen(), e.getX(), e.getY());
  }

  public void mouseExited(MouseEvent e) {
    mouseExited(e.getWhen(), e.getX(), e.getY());
  }

  public void mousePressed(MouseEvent e) {
    mousePressed(e.getWhen(), e.getX(), e.getY(), e.getModifiers(), e
        .isPopupTrigger());
  }

  public void mouseReleased(MouseEvent e) {
    mouseReleased(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
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
    mouseDragged(e.getWhen(), e.getX(), e.getY(), modifiers);
  }

  public void mouseMoved(MouseEvent e) {
    mouseMoved(e.getWhen(), e.getX(), e.getY(), e.getModifiers());
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    e.consume();
    mouseWheel(e.getWhen(), e.getWheelRotation(), e.getModifiers()
        | Event.MOUSE_WHEEL);
  }

  public void keyTyped(KeyEvent ke) {
    ke.consume();
    //if (!viewer.menuEnabled())
      //return;
    char ch = ke.getKeyChar();
    int modifiers = ke.getModifiers();
    // for whatever reason, CTRL may also drop the 6- and 7-bits,
    // so we are in the ASCII non-printable region 1-31
    if (Logger.debuggingHigh)
      Logger.debug("MouseManager keyTyped: " + ch + " " + (0+ch) + " " + modifiers);
    if (modifiers != 0 && modifiers != Event.SHIFT_MASK) {
      switch (ch) {
      case (char) 11:
      case 22:
      case 'v': // paste
        switch (modifiers) {
        case Event.CTRL_MASK:
//          String ret = viewer.getClipboardText();
//          if (ret == null)
//            break;
//          if (ret.startsWith("http://") && ret.indexOf("\n") < 0)
//            ret = "LoAd " + JSVEscape.eS(ret);
//          if (ret.startsWith("LoAd ")) {
//            //viewer.evalStringQuietSync(ret, false, true);
//            break;
//          }
//          //ret = viewer.loadInlineAppend(ret, false);
//          if (ret != null)
//            Logger.error(ret);
        }
        break;
      }
      return;
    }
    //if (!viewer.getBooleanProperty("allowKeyStrokes"))
      //return;
    addKeyBuffer(ke.getModifiers() == Event.SHIFT_MASK ? Character.toUpperCase(ch) : ch);
    
  }

  public void keyPressed(KeyEvent ke) {
    //if (viewer.isApplet())
      ke.consume();
    //actionManager.keyPressed(ke.getKeyCode(), ke.getModifiers());
  }

  public void keyReleased(KeyEvent ke) {
    ke.consume();
    //actionManager.keyReleased(ke.getKeyCode());
  }

  private String keyBuffer = "";

  private void clearKeyBuffer() {
    if (keyBuffer.length() == 0)
      return;
    keyBuffer = "";
  }

  private void addKeyBuffer(char ch) {
    if (ch == 10) {
      sendKeyBuffer();
      return;
    }
    if (ch == 8) {
      if (keyBuffer.length() > 0)
        keyBuffer = keyBuffer.substring(0, keyBuffer.length() - 1);
    } else {
      keyBuffer += ch;
    }
  }

  private void sendKeyBuffer() {
    String kb = keyBuffer;
    clearKeyBuffer();
    //viewer.evalStringQuietSync(kb, false, true);
  }

  private void mouseEntered(long time, int x, int y) {
    //actionManager.mouseEnterExit(time, x, y, false);
  }

  private void mouseExited(long time, int x, int y) {
    //actionManager.mouseEnterExit(time, x, y, true);
  }
/*
  void setMouseMode() {
    clearKeyBuffer();
    actionManager.setMouseMode();
  }
*/
  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param clickCount
   */
  private void mouseClicked(long time, int x, int y, int modifiers, int clickCount) {
    clearKeyBuffer();
    // clickedCount is not reliable on some platforms
    // so we will just deal with it ourselves
    //actionManager.mouseAction(Event.MOUSE_CLICKED, time, x, y, 1, modifiers);
  }

  private boolean isMouseDown; // Macintosh may not recognize CTRL-SHIFT-LEFT as drag, only move
  
  private void mouseMoved(long time, int x, int y, int modifiers) {
    clearKeyBuffer();
//    if (isMouseDown)
//      actionManager.mouseAction(Event.MOUSE_DRAGGED, time, x, y, 0, applyLeftMouse(modifiers));
//    else
//      actionManager.mouseAction(Event.MOUSE_MOVED, time, x, y, 0, modifiers);
  }

  private void mouseWheel(long time, int rotation, int modifiers) {
    clearKeyBuffer();
//    actionManager.mouseAction(Event.MOUSE_WHEELED, time, 0, rotation, 0, modifiers);
  }

  /**
   * 
   * @param time
   * @param x
   * @param y
   * @param modifiers
   * @param isPopupTrigger
   */
  private void mousePressed(long time, int x, int y, int modifiers,
                    boolean isPopupTrigger) {
    clearKeyBuffer();
    isMouseDown = true;
//    actionManager.mouseAction(Event.MOUSE_PRESSED, time, x, y, 0, modifiers);
  }

  private void mouseReleased(long time, int x, int y, int modifiers) {
    isMouseDown = false;
//    actionManager.mouseAction(Event.MOUSE_RELEASED, time, x, y, 0, modifiers);
  }

  private void mouseDragged(long time, int x, int y, int modifiers) {
    if ((modifiers & Event.MAC_COMMAND) == Event.MAC_COMMAND)
      modifiers = modifiers & ~Event.MOUSE_RIGHT | Event.CTRL_MASK; 
//    actionManager.mouseAction(Event.MOUSE_DRAGGED, time, x, y, 0, modifiers);
  }

  private static int applyLeftMouse(int modifiers) {
    // if neither BUTTON2 or BUTTON3 then it must be BUTTON1
    return ((modifiers & Event.BUTTON_MASK) == 0) ? (modifiers | Event.MOUSE_LEFT)
        : modifiers;
  }

  private int xWhenPressed, yWhenPressed, modifiersWhenPressed10;

}
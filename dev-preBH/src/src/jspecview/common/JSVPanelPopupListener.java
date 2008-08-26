/* Copyright (c) 2002-2007 The University of the West Indies
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <code>JSVPanelPopupListener</code> shows the popup menu on <code>JSVPanel</code>s
 * when the right mouse button is clicked.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class JSVPanelPopupListener extends MouseAdapter {
  private JSVPanelPopupMenu popup;
  private JSVPanel jsvp;
  private JDXSource source;

  /**
   * Constructor that takes the popup menu to be shown, the <code>JSVPanel</code>
   * and the Source of the Spectrum
   * @param popup the popup menu
   * @param jsvp the JSVPanel to place the popup on
   * @param source the JDXSource of the spectra displayed on the panel
   */
  public JSVPanelPopupListener(JSVPanelPopupMenu popup, JSVPanel jsvp, JDXSource source) {
    super();
    this.popup = popup;
    this.jsvp = jsvp;
    this.source = source;
  }

  /**
   * Oversides mousePressed in <code>MouseAdapter</code> class
   * @param e the <code>MouseEvent</code>
   */
  public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
  }

  /**
   * Overides mouseReleased in <code>MouseAdapter</code> class
   * @param e the <code>MouseEvent</code>
   */
  public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
  }

  /**
   * Shows the popop on the <code>JSVPanel</code>
   * @param e the <code>MouseEvent</code>
   */
  private void maybeShowPopup(MouseEvent e) {
    if (e.isPopupTrigger()) {
        popup.show(jsvp, e.getX(), e.getY());
        popup.setSelectedJSVPanel(jsvp);
        popup.setSource(source);
        popup.gridCheckBoxMenuItem.setSelected(jsvp.isGridOn());
        popup.coordsCheckBoxMenuItem.setSelected(jsvp.isCoordinatesOn());
        popup.revPlotCheckBoxMenuItem.setSelected(jsvp.isPlotReversed());
    }
  }

}

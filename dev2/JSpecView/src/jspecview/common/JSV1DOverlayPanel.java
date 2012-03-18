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

// CHANGES to 'JSVPanel.java'
// University of the West Indies, Mona Campus
//
// 25-06-2007 rjl - bug in ReversePlot for non-continuous spectra fixed
//                - previously, one point less than npoints was displayed
// 25-06-2007 cw  - show/hide/close modified
// 10-02-2009 cw  - adjust for non zero baseline in North South plots
// 24-08-2010 rjl - check coord output is not Internationalised and uses decimal point not comma
// 31-10-2010 rjl - bug fix for drawZoomBox suggested by Tim te Beek
// 01-11-2010 rjl - bug fix for drawZoomBox
// 05-11-2010 rjl - colour the drawZoomBox area suggested by Valery Tkachenko
// 23-07-2011 jak - Added feature to draw the x scale, y scale, x units and y units
//					independently of each other. Added independent controls for the font,
//					title font, title bold, and integral plot color.
// 24-09-2011 jak - Altered drawGraph to fix bug related to reversed highlights. Added code to
//					draw integration ratio annotations
// 03-06-2012 rmh - Full overhaul; code simplification; added support for Jcamp 6 nD spectra

package jspecview.common;

import java.awt.Color;
import java.util.List;
import jspecview.exception.JSpecViewException;
import jspecview.exception.ScalesIncompatibleException;
import jspecview.source.JDXSource;

/**
 * JSVPanel class draws a plot from the data contained a instance of a
 * <code>Graph</code>.
 * 
 * @see jspecview.common.Graph
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Craig A.D. Walters
 * @author Prof Robert J. Lancashire
 * @author Bob Hanson hansonr@stolaf.edu
 */

public class JSV1DOverlayPanel extends JSVPanel {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a <code>JSVPanel</code> with List of spectra and corresponding
   * start and end indices of data points that should be displayed
   * 
   * @param spectra
   *        the List of <code>Graph</code> instances
   * @param startIndices
   *        the start indices
   * @param endIndices
   *        the end indices
   * @throws JSpecViewException
   * @throws ScalesIncompatibleException
   */
  public JSV1DOverlayPanel(List<JDXSpectrum> spectra, int[] startIndices,
      int[] endIndices) throws ScalesIncompatibleException {
    if (!JDXSpectrum.areScalesCompatible(spectra))
      throw new ScalesIncompatibleException();
    initJSVPanel((Graph[]) spectra.toArray(new Graph[spectra.size()]), startIndices,
        endIndices);
  }

  /**
   * 
   * specifically for getIntegralPanel
   * 
   * @param spectra
   *        an array of spectra (<code>Spectrum</code>)
   * @throws ScalesIncompatibleException
   */
  private JSV1DOverlayPanel(Graph[] spectra,
                            JDXSource source,
                            JSVPanelPopupMenu popup) {
    this.source = source;
    this.popup = popup;
    initJSVPanel(spectra, null, null);
  }

  public static JSV1DOverlayPanel getIntegralPanel(JDXSpectrum spectrum,
                                                   Color color,
                                                   JDXSource source,
                                                   JSVPanelPopupMenu popup) {
    Graph graph = spectrum.getIntegrationGraph();
    JSV1DOverlayPanel jsvp = new JSV1DOverlayPanel(new Graph[] { spectrum,
        graph }, source, popup);
    jsvp.setTitle(graph.getTitle());
    jsvp.setPlotColors(new Color[] { jsvp.getPlotColor(0), color });
    return jsvp;
  }

}

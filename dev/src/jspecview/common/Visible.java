/* Copyright (c) 2008 The University of the West Indies
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

// CHANGES to 'visible.java' - module to predict colour of visible spectrum
// created July 2008 based on concept published by Darren L. Williams
// in J. Chem. Educ., 84(11), 1873-1877, 2007
//
// The CIE standard observer functions were curve fitted
// and the equations used for these calculations. The values obtained
// do not vary appreciably from those published in the JChemEd article
// University of the West Indies, Mona Campus

package jspecview.common;

import java.lang.Math;
import java.awt.Color;
import jspecview.util.Coordinate;
import jspecview.common.JDXSource;

/**
 * Visible class.
 * @author Craig Walters
 * @author Prof Robert J. Lancashire
 */

public class Visible {
  public static int blue = 399, red = 701, numVispoints;
  public static String Xunits="",Yunits="", redv, greenv, bluev;
  static int RED = 0, BLUE = 0, GREEN = 0;
  private static int ind400=0, ind505=0, ind700=0;
  public static double xspacing, firstX, lastX;
  public static JDXSource source;
  public static Color c;
  //public static Coordinate xyCoords[];
  public static double X, x1, Y, y1, Z, z1;
  private static double XUP, YUP, ZUP, XDWN, YDWN, ZDWN;
  private static double matrixx[]=new double[1000], matrixy[]=new double[1000]
      , matrixz[]=new double[1000], matrixcie[]=new double[1000];

  public Visible() {
  }

  public static String Colour(Coordinate xyCoords[]) {
    //xyCoords = source.getJDXSpectrum(0).getXYCoords();
    //Xunits   = source.getJDXSpectrum(0).getXUnits();
    //Yunits   = source.getJDXSpectrum(0).getYUnits();
    firstX   = xyCoords[0].getXVal();
    lastX    = xyCoords[xyCoords.length -1].getXVal();

    /*  double firstX = xyCoords[0].getXVal();
        double lastX = xyCoords[xyCoords.length - 1].getXVal();
        int npoints = xyCoords.length;
     */

    for (int i = 0; i < xyCoords.length; i++) {
      if (xyCoords[i].getXVal() < 401) {
        ind400 = i;
      }
      if (xyCoords[i].getXVal() < 506) {
        ind505 = i;
      }
      if (xyCoords[i].getXVal() < 701) {
        ind700 = i;
      }
    }
//    System.out.println("ind400="+ind400+" ind700="+ind700);
//    numVispoints= ind700 - ind400;
//    xspacing = (lastX - firstX) / xyCoords.length;

    if (firstX < 401 & lastX > 699 /*&Yunits.toLowerCase().contains("trans") &
         Xunits.toLowerCase().contains("nanometer") & || Xunits != "cm-1"*/) {

      for (int i = ind400; i < ind505; i++) { //change over at 505nm or 19800cm-1
        matrixx[(i-ind400)]=0.35*Math.exp(-0.001*(Math.pow((xyCoords[i].getXVal()-442), 2)));
        matrixy[(i-ind400)]=Math.exp(-0.00029*(Math.pow((xyCoords[i].getXVal()-558),2)));
        matrixz[(i-ind400)]=1.78*Math.exp(-0.00095*(Math.pow((xyCoords[i].getXVal()-448),2)));
        matrixcie[(i-ind400)]=115*Math.exp(-0.000012*(Math.pow((xyCoords[i].getXVal()-480),2)));
    //  System.out.println("datax: "+xyCoords[i].getXVal()+", matrixy: "+matrixy[i]+"i value = " +i);
      }
      for (int i = ind505; i < ind700; i++) {
        matrixx[(i-ind400)]=1.06053*Math.exp(-0.000439707*(Math.pow((xyCoords[i].getXVal()-596),2)));
        matrixy[(i-ind400)]=Math.exp(-0.00029*(Math.pow((xyCoords[i].getXVal()-558),2)));
        matrixz[(i-ind400)]=1.78*Math.exp(-0.00095*(Math.pow((xyCoords[i].getXVal()-448),2)));
        matrixcie[(i-ind400)]=115*Math.exp(-0.000012*(Math.pow((xyCoords[i].getXVal()-480),2)));
      }

      for (int i = ind400; i < ind700; i++) {
        XUP += (xyCoords[i].getYVal() * matrixx[(i - ind400)] * matrixcie[(i - ind400)]);
        XDWN += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
//System.out.println("matrixx: "+matrixx[i]+"; matrixy: "+matrixy[i]+"\n"+"matrixz: "+matrixz[i]+"; matrixcie: "+matrixcie[i]);
        YUP += (xyCoords[i].getYVal() * matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        YDWN += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
        ZUP += (xyCoords[i].getYVal() * matrixz[(i - ind400)] * matrixcie[(i - ind400)]);
        ZDWN += (matrixy[(i - ind400)] * matrixcie[(i - ind400)]);
      }

      X = XUP / XDWN;
      Y = YUP / YDWN;
      Z = ZUP / ZDWN;

      double sumXYZ = X + Y + Z;
      x1 = (X / (sumXYZ));
      y1 = (Y / (sumXYZ));
      z1 = (Z / (sumXYZ));

      double matrixRGB[] = new double[3];
      matrixRGB[0] = (X * 3.241) + (Y * ( -1.5374)) + (Z * ( -0.4986));
      matrixRGB[1] = (X * ( -0.9692)) + (Y * 1.876) + (Z * 0.0416);
      matrixRGB[2] = (X * 0.0556) + (Y * ( -0.204)) + (Z * 1.057);

      for (int i = 0; i < 3; i++) {
        if (matrixRGB[i] > 0.00304) {
          matrixRGB[i] = (1.055 * (Math.pow(matrixRGB[i], 1 / 2.4))) - 0.055;
        }
        else {
          matrixRGB[i] = 12.92 * matrixRGB[i];
        }
      }

      if (matrixRGB[0] < 0) {
        RED = 0;
      }
      else if (matrixRGB[0] > 1) {
        RED = 255;
      }
      else {
        RED = (int) Math.round(255 * matrixRGB[0]);
      }

      if (matrixRGB[1] < 0) {
        GREEN = 0;
      }
      else if (matrixRGB[1] > 1) {
        GREEN = 255;
      }
      else {
        GREEN = (int) Math.round(255 * matrixRGB[1]);
      }

      if (matrixRGB[2] < 0) {
        BLUE = 0;
      }
      else if (matrixRGB[2] > 1) {
        BLUE = 255;
      }
      else {
        BLUE = (int) Math.round(255 * matrixRGB[2]);
      }

//       RGBtoHex(RED,GREEN,BLUE) {toHex(RED)+toHex(G)+toHex(B)}
//       N=Math.max(0,N); N=Math.min(N,255); N=Math.round(N);
      redv = "" + ("0123456789ABCDEF".charAt( (RED - RED % 16) / 16)) +
          ("0123456789ABCDEF".charAt(RED % 16));
      greenv = "" + ("0123456789ABCDEF".charAt( (GREEN - GREEN % 16) / 16)) +
          ("0123456789ABCDEF".charAt(GREEN % 16));
      bluev = "" + ("0123456789ABCDEF".charAt( (BLUE - BLUE % 16) / 16)) +
          ("0123456789ABCDEF".charAt(BLUE % 16));

//      System.out.println("#"+ redv + greenv + bluev);
      XUP=0; XDWN=0;
      YUP=0; YDWN=0;
      ZUP=0; ZDWN=0;

      //return ("#" + redv + greenv + bluev);
      return(""+RED+","+GREEN+","+BLUE);
    }
    else {

      return null;

    }
  }
}


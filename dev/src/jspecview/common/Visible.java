/* Copyright (c) 2008-2009 The University of the West Indies
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
// Judd-Vos-modified 1931 CIE 2-deg color matching functions (1978)
// The CIE standard observer functions were curve fitted using FitYK
// and the equations used for these calculations. The values obtained
// do not seem to vary appreciably from those published in the JChemEd article

package jspecview.common;

import java.lang.Math;

/**
 * Visible class - for prediction of colour from visible spectrum
 * @author Craig Walters
 * @author Prof Robert J. Lancashire
 */

public class Visible {
  public static int blue = 399, red = 701, numVispoints;
  public static String Xunits="",Yunits, redv, greenv, bluev;
  static int RED = 0, BLUE = 0, GREEN = 0;
  private static int ind400=0, ind437=0, ind499=0, ind700=0;
  public static double xspacing, firstX, lastX;
  //public static JDXSource source;
  //public static Color c;
 // public static Coordinate xyCoords[];
  public static double X, x1, Y, y1, Z, z1;
  private static double XUP, YUP, ZUP, XDWN, YDWN, ZDWN;
  private static double matrixx[]=new double[1000], matrixy[]=new double[1000]
      , matrixz[]=new double[1000], matrixcie[]=new double[1000];

  public Visible() {
  }

  public static String Colour(Coordinate xyCoords[],String Yunits) {

    firstX   = xyCoords[0].getXVal();
    lastX    = xyCoords[xyCoords.length -1].getXVal();

      for (int i = 0; i < xyCoords.length; i++) {
        if (xyCoords[i].getXVal() < 401) {
          ind400 = i;
        }
        if (xyCoords[i].getXVal() < 438) {
          ind437 = i;
        }
        if (xyCoords[i].getXVal() < 500) {
          ind499 = i;
        }
        if (xyCoords[i].getXVal() < 701) {
          ind700 = i;
        }
      }

    if((ind700 - ind400) >30 & firstX < 401 & lastX > 699){
// treat the x-bar and CIE D65 curves in two parts with the changeover at 499 nm
      for (int i = ind400; i < ind437; i++) { 
        matrixx[ (i - ind400)] = 0.335681 * Math.exp( -0.000998224 * (Math.pow( (xyCoords[i].getXVal() - 441.96), 2)));
        matrixy[ (i - ind400)] = 1.01832 * Math.exp( -0.000284660 * (Math.pow( (xyCoords[i].getXVal() - 559.04), 2)));
        matrixz[ (i - ind400)] = 1.63045 * Math.exp( -0.001586000 * (Math.pow( (xyCoords[i].getXVal() - 437.406), 2)));
        matrixcie[(i - ind400)]= 115.195 * Math.exp( -8.33988E-05 * (Math.pow( (xyCoords[i].getXVal() - 472.727), 2)));
            }
      for (int i = ind437; i < ind499; i++) { //change over at 437nm for z
        matrixx[ (i - ind400)] = 0.335681 * Math.exp( -0.000998224 * (Math.pow( (xyCoords[i].getXVal() - 441.96), 2)));
        matrixy[ (i - ind400)] = 1.01832 * Math.exp( -0.00028466 * (Math.pow( (xyCoords[i].getXVal() - 559.04), 2)));
        matrixz[ (i - ind400)] = 1.63045 * Math.exp( -0.00043647 * (Math.pow( (xyCoords[i].getXVal() - 437.406), 2)));
        matrixcie[(i - ind400)]= 115.195 * Math.exp( -8.33988E-05 * (Math.pow( (xyCoords[i].getXVal() - 472.727), 2)));
                  }
      for (int i = ind499; i < ind700; i++) { //change over at 500nm for x and CIE-D65
        matrixx[ (i - ind400)] = 1.05583 * Math.exp( -0.00044156 * (Math.pow( (xyCoords[i].getXVal() - 596.124), 2)));
        matrixy[ (i - ind400)] = 1.01832 * Math.exp( -0.00028466 * (Math.pow( (xyCoords[i].getXVal() - 559.04), 2)));
        matrixz[ (i - ind400)] = 1.63045 * Math.exp( -0.00043647 * (Math.pow( (xyCoords[i].getXVal() - 437.406), 2)));
        matrixcie[(i - ind400)]= 208.375 - (0.195278 * (xyCoords[i].getXVal()));
          }
      }else{
      return null;
      }
      if (Yunits.toLowerCase().contains("trans")) {
        for (int i = ind400; i < ind700; i++) {
          XUP += (xyCoords[i].getYVal() * matrixx[ (i - ind400)] *  matrixcie[ (i - ind400)]);
          XDWN += (matrixy[ (i - ind400)] * matrixcie[ (i - ind400)]);
          YUP += (xyCoords[i].getYVal() * matrixy[ (i - ind400)] *  matrixcie[ (i - ind400)]);
          YDWN += (matrixy[ (i - ind400)] * matrixcie[ (i - ind400)]);
          ZUP += (xyCoords[i].getYVal() * matrixz[ (i - ind400)] *  matrixcie[ (i - ind400)]);
          ZDWN += (matrixy[ (i - ind400)] * matrixcie[ (i - ind400)]);
        }
      }else {
        for (int i = ind400; i <= ind700 ; i++) {
          if(xyCoords[i].getYVal() < 0){
            xyCoords[i].setYVal(0.0);
          }
        XUP  += (Math.pow(10, -xyCoords[i].getYVal())* matrixx[(i - ind400)] *  matrixcie[ (i - ind400)]);
        XDWN += (matrixy[ (i - ind400)] * matrixcie[(i - ind400)]);
        YUP  += (Math.pow(10, -xyCoords[i].getYVal())* matrixy[(i - ind400)] *  matrixcie[ (i - ind400)]);
        YDWN += (matrixy[ (i - ind400)] * matrixcie[(i - ind400)]);
        ZUP  += (Math.pow(10, -xyCoords[i].getYVal())* matrixz[(i - ind400)] *  matrixcie[ (i - ind400)]);
        ZDWN += (matrixy[ (i - ind400)] * matrixcie[ (i - ind400)]);
          }
        }

        X = XUP / XDWN;
        Y = YUP / YDWN;
        Z = ZUP / ZDWN;

        double sumXYZ = X + Y + Z;
        x1 = (X / (sumXYZ));
        y1 = (Y / (sumXYZ));
        z1 = (Z / (sumXYZ));

    //    System.out.println("x1 = "+x1+", y1 = "+y1+", z1 = "+z1);

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
        }else if (matrixRGB[0] > 1) {
          RED = 255;
        }else {
          RED = (int) Math.round(255 * matrixRGB[0]);
        }

        if (matrixRGB[1] < 0) {
          GREEN = 0;
        }else if (matrixRGB[1] > 1) {
          GREEN = 255;
        }else {
          GREEN = (int) Math.round(255 * matrixRGB[1]);
        }

        if (matrixRGB[2] < 0) {
          BLUE = 0;
        }else if (matrixRGB[2] > 1) {
          BLUE = 255;
        }else {
          BLUE = (int) Math.round(255 * matrixRGB[2]);
        }

        redv = "" + ("0123456789ABCDEF".charAt( (RED - RED % 16) / 16)) +
            ("0123456789ABCDEF".charAt(RED % 16));
        greenv = "" + ("0123456789ABCDEF".charAt( (GREEN - GREEN % 16) / 16)) +
            ("0123456789ABCDEF".charAt(GREEN % 16));
        bluev = "" + ("0123456789ABCDEF".charAt( (BLUE - BLUE % 16) / 16)) +
            ("0123456789ABCDEF".charAt(BLUE % 16));

//      System.out.println("#"+ redv + greenv + bluev);
        XUP = 0;
        XDWN = 0;
        YUP = 0;
        YDWN = 0;
        ZUP = 0;
        ZDWN = 0;

        //return ("#" + redv + greenv + bluev);
        return ("" + RED + "," + GREEN + "," + BLUE);
      }
  }


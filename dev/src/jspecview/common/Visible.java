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
import jspecview.util.Coordinate;
//import java.awt.Color;
//import jspecview.common.JDXSource;

/**
 * Visible class - for prediction of colour from visible spectrum
 * @author Craig Walters
 * @author Prof Robert J. Lancashire
 */

public class Visible {
  public static int blue = 399, red = 701, numVispoints;
  public static String Xunits="",Yunits, redv, greenv, bluev;
  static int RED = 0, BLUE = 0, GREEN = 0;
  private static int ind400=0, ind505=0, ind700=0;
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
    //Yunits = source.getJDXSpectrum(0).getYUnits();
    //xyCoords = source.getJDXSpectrum(0).getXYCoords();
    //Xunits   = source.getJDXSpectrum(0).getXUnits();
    //Yunits   = source.getJDXSpectrum(0).getYUnits();
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
//    System.out.println("ind400=" + ind400 + ", ind700=" + ind700 +", firstX= "+firstX);
//    System.out.println("Y values " + xyCoords[2].getYVal());

    if((ind700 - ind400) >30 & firstX < 401 & lastX > 699){
//    numVispoints= ind700 - ind400;
//    xspacing = (lastX - firstX) / xyCoords.length;

/*      if (firstX < 401 & lastX > 699 &Yunits.toLowerCase().contains("trans") &
           Xunits.toLowerCase().contains("nanometer") & || Xunits != "cm-1") {*/
      for (int i = ind400; i < ind505; i++) { //change over at 505nm or 19800cm-1
        matrixx[ (i - ind400)] = 0.397426 * Math.exp( -0.00121037 * (Math.pow( (xyCoords[i].getXVal() - 444.542), 2)));
        matrixy[ (i - ind400)] = 1.01041 * Math.exp( -0.000235244 * (Math.pow( (xyCoords[i].getXVal() - 556.124), 2)));
        matrixz[ (i - ind400)] = 2.06586 * Math.exp( -0.000991365 * (Math.pow( (xyCoords[i].getXVal() - 448.585), 2)));
        matrixcie[(i - ind400)] =2.62569E-13* Math.pow(xyCoords[i].getXVal(),6) - 9.52467E-10 * Math.pow(xyCoords[i].getXVal(),5) + 1.40776E-06 * Math.pow(xyCoords[i].getXVal(),4) - 1.07900E-03* Math.pow(xyCoords[i].getXVal(),3) + 4.48632E-01* Math.pow(xyCoords[i].getXVal(),2) - (9.48927E+01 * xyCoords[i].getXVal()) + 7.95116E+03;
        //matrixcie[(i - ind400)] = -(2.034308E-09 * Math.pow(xyCoords[i].getXVal(),6)) + (5.602520E-06 * Math.pow(xyCoords[i].getXVal(),5)) - (6.416264E-03 * Math.pow(xyCoords[i].getXVal(),4)) + (3.911228* Math.pow(xyCoords[i].getXVal(),3)) - (1.338422E+03* Math.pow(xyCoords[i].getXVal(),2)) + (2.437787E+05 * xyCoords[i].getXVal()) - 1.846333E+07;
       //-2.034308E-09x6 + 5.602520E-06x5 - 6.416264E-03x4 + 3.911228E+00x3 - 1.338422E+03x2 + 2.437787E+05x - 1.846333E+07
       //1.0989E-07x5 - 2.4771E-04x4 + 2.2288E-01x3 - 1.0007E+02x2 + 2.2419E+04x - 2.0052E+06
       //      System.out.println("matrixcie: "+matrixcie[i]+"; i value = " +i);
            }
      for (int i = ind505; i < ind700; i++) {
        matrixx[ (i - ind400)] = 1.12885 * Math.exp( -0.000419044 * (Math.pow( (xyCoords[i].getXVal() - 593.194), 2)));
        matrixy[ (i - ind400)] = 1.01041 * Math.exp( -0.000235244 *(Math.pow( (xyCoords[i].getXVal() - 556.124), 2)));
        matrixz[ (i - ind400)] = 2.06586 * Math.exp( -0.000991365 * (Math.pow( (xyCoords[i].getXVal() - 448.585), 2)));
        matrixcie[(i - ind400)] =2.62569E-13* Math.pow(xyCoords[i].getXVal(),6) - 9.52467E-10 * Math.pow(xyCoords[i].getXVal(),5) + 1.40776E-06 * Math.pow(xyCoords[i].getXVal(),4) - 1.07900E-03* Math.pow(xyCoords[i].getXVal(),3) + 4.48632E-01* Math.pow(xyCoords[i].getXVal(),2) - (9.48927E+01 * xyCoords[i].getXVal()) + 7.95116E+03;
 //       matrixcie[ (i - ind400)] = (-1.17537E-07 * Math.pow(xyCoords[i].getXVal(),4)) + (2.85933E-04* Math.pow(xyCoords[i].getXVal(),3)) - (2.59716E-01* Math.pow(xyCoords[i].getXVal(),2)) + (1.04189E+02 * xyCoords[i].getXVal()) - 1.54543E+04;
       // System.out.println("matrixcie: "+matrixcie[i]+"; i value = " +i);
       // -1.17537E-07x4 + 2.85933E-04x3 - 2.59716E-01x2 + 1.04189E+02x - 1.54543E+04
        //2.6632E-11x6 - 9.5984E-08x5 + 1.4363E-04x4 - 1.1421E-01x3 + 5.0894E+01x2 - 1.2051E+04x + 1.1848E+06
            //115 * Math.exp( -0.000012 * (Math.pow( (xyCoords[i].getXVal() - 480), 2)));
            //5.9924E-08 * Math.pow(xyCoords[i].getXVal(),4) + 1.4172E-04 *Math.pow(xyCoords[i].getXVal(),3) - 1.2499E-01 *Math.pow(xyCoords[i].getXVal(),2) + 4.8519E+01 *xyCoords[i].getXVal() - 6.8703E+03
            //2.62569E-13* Math.pow(xyCoords[i].getXVal(),6) - 9.52467E-10 * Math.pow(xyCoords[i].getXVal(),5) + 1.40776E-06 * Math.pow(xyCoords[i].getXVal(),4) - 1.07900E-03* Math.pow(xyCoords[i].getXVal(),3) + 4.48632E-01* Math.pow(xyCoords[i].getXVal(),2) - (9.48927E+01 * xyCoords[i].getXVal()) + 7.95116E+03;
          }
      }else{
      return null;
      }
      if (Yunits.toLowerCase().contains("trans")) {
        for (int i = ind400; i < ind700; i++) {
          XUP += (xyCoords[i].getYVal() * matrixx[ (i - ind400)] *
                  matrixcie[ (i - ind400)]);
          XDWN += (matrixy[ (i - ind400)] * matrixcie[ (i - ind400)]);
//System.out.println("matrixx: "+matrixx[i]+"; matrixy: "+matrixy[i]+"\n"+"matrixz: "+matrixz[i]+"; matrixcie: "+matrixcie[i]);
          YUP += (xyCoords[i].getYVal() * matrixy[ (i - ind400)] *
                  matrixcie[ (i - ind400)]);
          YDWN += (matrixy[ (i - ind400)] * matrixcie[ (i - ind400)]);
          ZUP += (xyCoords[i].getYVal() * matrixz[ (i - ind400)] *
                  matrixcie[ (i - ind400)]);
          ZDWN += (matrixy[ (i - ind400)] * matrixcie[ (i - ind400)]);
        }
      }else {
        for (int i = ind400; i <= ind700 ; i++) {
          if(xyCoords[i].getYVal() < 0){
            xyCoords[i].setYVal(0.0);
          }
        XUP  += (Math.pow(10, -xyCoords[i].getYVal())* matrixx[(i - ind400)] *
                 matrixcie[ (i - ind400)]);
        XDWN += (matrixy[ (i - ind400)] * matrixcie[(i - ind400)]);
//System.out.println("matrixx: "+matrixx[i]+"; matrixy: "+matrixy[i]+"\n"+"matrixz: "+matrixz[i]+"; matrixcie: "+matrixcie[i]);
        YUP  += (Math.pow(10, -xyCoords[i].getYVal())* matrixy[(i - ind400)] *
                 matrixcie[ (i - ind400)]);
        YDWN += (matrixy[ (i - ind400)] * matrixcie[(i - ind400)]);
        ZUP  += (Math.pow(10, -xyCoords[i].getYVal())* matrixz[(i - ind400)] *
                 matrixcie[ (i - ind400)]);
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


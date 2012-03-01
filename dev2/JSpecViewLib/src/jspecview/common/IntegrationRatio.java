/* Copyright (c) 2002-2008 The University of the West Indies
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

// CHANGES to 'IntegrationRatio.java' - Integration Ratio Representation
// University of the West Indies, Mona Campus
// 24-09-2011 jak - Created class as an extension of the Coordinate class
//					to handle the integration ratio value.

package jspecview.common;

import java.text.DecimalFormat;

import jspecview.util.TextFormat;

/**
 * The <code>IntegrationRatio</code> class stores the x and y values of an
 * integration ratio as well as its value.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class IntegrationRatio extends Coordinate
{
	/** the integral value */
    double integralVal = 0;
	
	/** the format of the string returned by getXString() and getYString() */
    private static DecimalFormat formatter = TextFormat.getDecimalFormat("0.########");
    
    /**
     * Constructor
     */
    public IntegrationRatio() {}
    
    /**
     * Constructor
     * @param x the x value
     * @param y the y value
     */
    public IntegrationRatio(double x, double y) {
    	super(x,y);
    }
    
    /**
     * Constructor
     * @param x the x value
     * @param y the y value
     * @param integralValue the integral value
     */
    public IntegrationRatio(double x, double y, double integralValue) {
        super(x,y);
        integralVal = integralValue;
    }

	/**
     * Returns the integral ratio
     * @return
     */
    public double getIntegralVal(){
    	return integralVal;
    }
    
    /**
     * Return the integral ratio formatted to a maximum of eight decimal
     * places
     * @return Returns the integral ratio formatted to a maximum of eight
     * decimal places
     */
    public String getIntegralString(){
    	return formatter.format(integralVal);
    }

    /**
     * Sets the integral ratio
     * @param val the integral ratio
     */
    public void setIntegralVal(double val){
    	integralVal = val;
    }
    
    /**
     * Returns a new IntegrationRatio that has the same x, y and integration values
     * of this integration ratio
     * @return Returns a new IntegrationRatio that has the same x, y and integration
     * values of this integration ratio
     */
    @Override
    public IntegrationRatio copy(){
    	double x = getXVal();
    	double y = getYVal();
    	return new IntegrationRatio(x, y, integralVal);
    }
    
    /**
     * Indicates whether some other Integration Ratio is equal to this one
     * @param coord the reference coordinate
     * @return true if the coordinates are equal, false otherwise
     */
    @Override
    public boolean equals(Object otherObject){
      return (this == otherObject 
          || otherObject instanceof IntegrationRatio
          && ((Coordinate) otherObject).equals(this)
          && integralVal == ((IntegrationRatio)otherObject).integralVal);
    }

    /**
     * Overrides Objects toString() method
     * @return the String representation of this coordinate
     */
    @Override
    public String toString(){
      return "[" + getXVal() + ", " + getYVal() + "," + integralVal + "]";
    }
}



/**
 * 
 */
package jspecview.common;

import java.util.Comparator;

public class CoordComparator implements Comparator<Coordinate> {
  public int compare(Coordinate c1, Coordinate c2) {
    return (c1.getXVal() > c2.getXVal() ? 1 : c1.getXVal() < c2.getXVal() ? -1 : 0);
  }    
}
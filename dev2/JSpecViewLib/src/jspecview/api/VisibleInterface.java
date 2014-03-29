package jspecview.api;

import jspecview.common.Coordinate;

public interface VisibleInterface {

	String getColour(Coordinate[] xyCoords, boolean isAbsorbance, boolean useFitted);

}

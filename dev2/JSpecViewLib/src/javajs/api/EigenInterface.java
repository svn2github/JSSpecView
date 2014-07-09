package javajs.api;

import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.Quat;

public interface EigenInterface {

  float getRmsd(P3[][] centerAndPoints, Quat q);

  P3[] getCenterAndPoints(Lst<P3> pts);

}

package jspecview.common;

import java.util.Comparator;

public class IntegralComparator implements Comparator<Measurement> {

	public int compare(Measurement m1, Measurement m2) {
		return (m1.getXVal() < m2.getXVal() ? -1 : m1.getXVal() > m2.getXVal() ? 1 : 0);
	}

}

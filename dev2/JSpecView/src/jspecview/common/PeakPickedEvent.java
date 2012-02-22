package jspecview.common;

import java.util.EventObject;

import jspecview.common.Coordinate;

@SuppressWarnings("serial")
public class PeakPickedEvent extends EventObject {
	
	private Coordinate coord;
	private String peakInfo;
	
	public PeakPickedEvent(Object source, Coordinate coord, String peakInfo) {
		super(source);
		this.coord = coord;
		this.peakInfo = peakInfo;
	}

	public Coordinate getCoord() {
		return coord;
	}

	public String getPeakInfo() {
		return peakInfo;
	}

}

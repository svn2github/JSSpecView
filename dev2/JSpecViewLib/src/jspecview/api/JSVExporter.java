package jspecview.api;

import org.jmol.io.JmolOutputChannel;

import jspecview.common.ExportType;
import jspecview.common.JDXSpectrum;
import jspecview.common.JSViewer;
import jspecview.common.PanelData;

public interface JSVExporter {

	/**
	 * 
	 * @param viewer
	 * @param type 
	 * @param out
	 * @param spec  not relevant for PDF, JPG, PNG
	 * @param startIndex  not relevant for PDF, JPG, PNG
	 * @param endIndex  not relevant for PDF, JPG, PNG
	 * @param pd only for SVG/SVGI
	 * @return message or text
	 * @throws Exception
	 */
	String exportTheSpectrum(JSViewer viewer, ExportType type,
			JmolOutputChannel out, JDXSpectrum spec, int startIndex, int endIndex, PanelData pd) throws Exception;

}
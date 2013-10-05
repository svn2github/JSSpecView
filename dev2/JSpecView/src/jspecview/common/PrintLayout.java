package jspecview.common;

/**
 * <code>PrintLayout</code> class stores all the information needed from the
 * <code>PrintLayoutDialog</code>
 */

public class PrintLayout {
	/**
	 * The paper orientation ("portrait" or "landscape")
	 */
	public String layout = "landscape";
	/**
	 * The position of the graph on the paper
	 * ("center", "default", "fit to page")
	 */
	public String position = "fit to page";
	/**
	 * whether or not the grid should be printed
	 */
	public boolean showGrid = true;
	/**
	 * whether or not the X-scale should be printed
	 */
	public boolean showXScale = true;
	/**
	 * whether or not the Y-scale should be printed
	 */
	public boolean showYScale = true;
	/**
	 * whether or not the title should be printed
	 */
	public boolean showTitle = true;
	/**
	 * The font of the elements
	 */
	public String font;
	/**
	 * The size of the paper to be printed on
	 */
	public Object paper;
	public boolean asPDF;

}
package javajs.swing;

import javajs.awt.JsColor;
import javajs.lang.StringBuffer;


public class JDialog extends JContainer {

	private static final int headerHeight = 25;
	private int defaultWidth = 600;
	private int defaultHeight = 300;
	
	private JContentPane contentPane;
	private String title;
	private String html;
	
	int[] loc;

	public JDialog() {
		super("JD");
		add(contentPane = new JContentPane());
		setBackground(JsColor.get3(210, 210, 240));
		contentPane.setBackground(JsColor.get3(230, 230, 230));
	}
	
	public void setLocation(int[] loc) {
		this.loc = loc;
	}
	
	public JContentPane getContentPane() {
		return contentPane;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void validate() {
		setDialog();
	}

	public void dispose() {
		{
			
			/**
			 * @j2sNative
			 * 
			 * Jmol.Dialog.dispose(this);
			 * 
			 */
			{
			}
			
		}
	}

	public void pack() {
		setDialog();
	}

  public void repaint() {
  	setDialog();
  	// TODO not sure this is necessary.
	}
  
  /**
   * Set it into DOM, but don't show it yet.
   * this.loc, this.manager, this.id, etc.
   * 
   */
	private void setDialog() {
		html = toHTML();
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.setDialog(this);
		 * 
		 * 
		 */
		{
			System.out.println(html);
		}
	}
	
	@Override
	public String toHTML() {
		renderWidth = getSubcomponentWidth();
		if (renderWidth == 0)
			renderWidth = defaultWidth;
		renderHeight = contentPane.getSubcomponentHeight();
		if (renderHeight == 0)
			renderHeight = defaultHeight;
		int h = renderHeight - headerHeight;
		StringBuffer sb = new StringBuffer();
		sb.append("\n<div id='" + id + "' class='JDialog' style='" + getCSSstyle(0) + "position:relative;top:0px;left:0px;reize:both;'>\n");
	  sb.append("\n<div id='" + id + "_title' class='JDialogTitle' style='width:100%;height:25px;padding:5px 5px 5px 5px;height:"+headerHeight+"px'>"
	  		+"<span style='text-align:center;'>" + title + "</span><span style='position:absolute;text-align:right;right:1px;>"
	  		+ "<a id='" + id + "_closer' href='javascript:Jmol.Dialog.windowClosing(this)'>[x]</a></span></div>\n");
	  sb.append("\n<div id='" + id + "_body' class='JDialogBody' style='width:100%;height:"+h+"px;"
	  		+"position:	relative;left:0px;top:0px'>\n");
	  sb.append(contentPane.toHTML());
		sb.append("\n</div></div>\n");
		return sb.toString();
	}



}
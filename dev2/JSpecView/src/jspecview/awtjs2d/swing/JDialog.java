package jspecview.awtjs2d.swing;

import jspecview.awtjs2d.JsColor;

import org.jmol.util.SB;

public class JDialog extends JComponent {

	private static final int headerHeight = 25;
	private int defaultWidth = 500;
	private int defaultHeight = 300;
	
	private JContentPane contentPane;
	private String title;
	private String html;
	
	int[] loc;

	public JDialog() {
		super("JD");
		contentPane = new JContentPane();
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
		if (width == 0)
			width = defaultWidth;
		if (height == 0)
			height = defaultHeight;
		
		int h = getHeight() - headerHeight;
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JDialog' style='" + getCSSstyle(0) + "position:relative;top:0px;left:0px;reize:both;'>\n");
	  sb.append("\n<div id='" + id + "_title' class='JDialogTitle' style='width:100%;height:25px;padding:5px 5px 5px 5px;height:"+headerHeight+"px'>"
	  		+"<span style='text-align:center;'>" + title + "</span><span style='position:absolute;text-align:right;right:1px;>"
	  		+ "<a href='javascript:Jmol.Dialog.dispose(this)'>[x]</a></span></div>\n");
	  sb.append("\n<div id='" + id + "_body' class='JDialogBody' style='width:100%;height:"+h+"px;"
	  		+"position:relative;left:0px;top:0px'>\n");
	  sb.append(contentPane.toHTML());
		sb.append("\n</div></div>\n");
		return sb.toString();
	}



}

package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JDialog extends JComponent {

	private JContentPane contentPane;
	private String title;

	private int[] loc;
	
	public JDialog() {
		super();
		this.contentPane = new JContentPane();
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
		showHTML();
	}

	public void dispose() {
		{
			
		}
	}

	public void pack() {
		showHTML();
	}

  public void repaint() {
  	showHTML();
	}
  
	private void showHTML() {
		String html = toHTML();
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.addDialog(this, html);
		 * 
		 * 
		 */
		{
			System.out.println(html);
		}
	}

	@Override
	public String toHTML() {
		String id = registerMe("JD");
		SB sb = new SB();
		sb.append("\n<div id='" + id + "' class='JDialog' style='" + getCSSstyle(true) + ";position:relative;background-color:yellow'>\n");
	  sb.append("\n<div id='" + id + "_title' class='JDialogTitle' style='width:100%;height:20px'><span>" + title + "</span><span style='position:absolute;text-align:right;right:1px;><a href='javascript:Jmol.Dialog.dispose(this)'>[x]</a></span></div>\n");
	  sb.append(contentPane.toHTML());
		sb.append("\n</div>\n");
		return sb.toString();
	}

}

package jspecview.awtjs2d.swing;

import jspecview.api.JSVColor;
import jspecview.dialog.DialogManager;
import jspecview.util.JSVColorUtil;

abstract public class JComponent {
	
	protected boolean visible;	
	protected boolean enabled;
	protected String text;
	protected String name;
	protected int width;
	protected int height;
	protected DialogManager dialogManager;
	protected String id;
	
	private JSVColor bgcolor;

	protected JComponent(String type) {
		/**
		 * @j2sNative
		 *            
		 *            Jmol.Dialog.register(this, type);
		 */
		{
			id = type + ("" + Math.random()).substring(3);
		}

	}
	
	abstract public String toHTML();
	
	public void setBackground(JSVColor color) {
		bgcolor = color;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPreferredSize(Dimension dimension) {
		this.width = dimension.width;
		this.height = dimension.height;		
	}

	public void addActionListener(DialogManager manager) {
		this.dialogManager = manager;
	}

	public String getText() {
		return text;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.setVisible(this);
		 * 
		 */
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	protected String getCSSstyle(int defaultPercent) {		
		if (width == 155)
			System.out.println("hmm");
		return (width > 0 ? "width:" + width +"px;" : defaultPercent > 0 ? "width:"+defaultPercent+"%;" : "")
		+ (height > 0 ?"height:" + height + "px;" : defaultPercent > 0 ? "height:"+defaultPercent+"%;" : "")
		+ (bgcolor == null ? "" : "background-color:" + JSVColorUtil.colorToCssString(bgcolor) + ";");
	}
}

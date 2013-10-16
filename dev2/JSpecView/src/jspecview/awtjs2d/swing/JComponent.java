package jspecview.awtjs2d.swing;

import jspecview.dialog.DialogManager;
import jspecview.util.JSVColor;
import jspecview.util.JSVColorUtil;

abstract public class JComponent {

	static int nComponents;
	
	public JSVColor bgcolor;

	protected boolean visible;	
	protected boolean enabled;
	protected String text;
	protected String name;
	protected int width;
	protected int height;
	protected DialogManager dialogManager;
	private int id;
	private boolean registered;

	public JComponent() {
		id = ++nComponents;
	}
	
	abstract public String toHTML();
	
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
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	protected String getCSSstyle(boolean default100) {		
		return (width > 0 ? ";width:" + width +"px;" : default100 ? "width:100%" : "")
		+ (height > 0 ?";height:" + height + "px" : default100 ? ";height:100%" : "")
		+ (bgcolor == null ? "" : ";background-color:" + JSVColorUtil.colorToCssString(bgcolor));
	}
	
	protected String registerMe(String type) {
		/**
		 * @j2sNative
		 * 
		 * if (!this.registered) {
		 * Jmol.Dialog.register(this, type);
		 * this.regitered = true;
		 * }
		 */
		{
		}
		return type + id;
	}




}

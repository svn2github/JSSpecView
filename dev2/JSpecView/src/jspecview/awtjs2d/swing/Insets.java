package jspecview.awtjs2d.swing;

public class Insets {

	int top, left, bottom, right;
	
	public Insets(int top, int left, int bottom, int right) {
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
	}

	public String getStyle(int ipadx, int ipady) {
		return "margin:" + top + "px " + (ipadx + left) + "px " + bottom + "px " + (ipady + right) + "px;height:100%";
	}

}

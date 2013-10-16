package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JCheckBox extends JComponent {

	private boolean selected;

	public void setSelected(boolean selected) {
		this.selected = selected;
		String id = registerMe("JCkB");
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.setSelected(id, selected, false);
		 * 
		 */
		{
			System.out.println(id + "  " + selected);
		}
	}

	public boolean isSelected() {
		return selected;
	}

	@Override
	public String toHTML() {
		String id = registerMe("JCkB");
		SB sb = new SB();
		sb.append("<input type=checkbox id='" + id + "' class='JCheckBox' style='" + getCSSstyle(false) + "' onclick='Jmol.Dialog.click(this)'>");
		return sb.toString();
	}


}

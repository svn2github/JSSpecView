package javajs.swing;

import javajs.lang.StringBuilder;

public class JComboBox<T>  extends JComponent {

	private String[] info;
	private int selectedIndex;

	public JComboBox(String[] info){
		super("cmbJCB");
		this.info = info;
	}

	public void setSelectedIndex(int i) {
		selectedIndex = i;
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.setSelectedIndex(this);
		 * 
		 */
		{
		}
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public Object getSelectedItem() {
		return (selectedIndex < 0 ? null : info[selectedIndex]);
	}

	@Override
	public String toHTML() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n<select id='JCmB" + id + "' class='JComboBox' onchange='Jmol.Dialog.click(this)'>\n");		
		for (int i = 0; i < info.length; i++)
			sb.append("\n<option class='JComboBox_option'" + (i == selectedIndex ? "selected":"") + ">" + info[i] + "</option>");
		sb.append("\n</select>\n");
		return sb.toString();
	}



}

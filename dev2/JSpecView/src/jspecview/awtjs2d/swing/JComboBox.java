package jspecview.awtjs2d.swing;

import org.jmol.util.SB;

public class JComboBox<T>  extends JComponent {

	private String[] info;
	private int selectedIndex;

	public JComboBox(String[] info){
		this.info = info;
	}

	public void setSelectedIndex(int i) {
		this.selectedIndex = i;
		String id = registerMe("JCmB");
		/**
		 * @j2sNative
		 * 
		 * Jmol.Dialog.setSelectedIndex(id, i, false);
		 * 
		 */
		{
			System.out.println(id + "  " + selectedIndex);
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
		String id = registerMe("JCmB");
		SB sb = new SB();
		sb.append("\n<select id='JCmB" + id + "' class='JComboBox' onchange='Jmol.Dialog.click(this)'>\n");		
		for (int i = 0; i < info.length; i++)
			sb.append("\n<option class='JComboBox_option'" + (i == selectedIndex ? "selected":"") + ">" + info[i] + "</option>");
		sb.append("\n</select>\n");
		return sb.toString();
	}



}

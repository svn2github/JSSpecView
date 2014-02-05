package javajs.swing;

public class JRadioButtonMenuItem extends JMenuItem {

  public JRadioButtonMenuItem() {
    super("rad");
  }

  @Override
  public String htmlLabel() {
    return   "<input id=\"ID-rb\" type=\"radio\" name=\"" + this.htmlName + "\" " 
        + (this.selected ? "checked" : "") + " /><label for=\"ID-rb\">TeXt</label>";
  }

}

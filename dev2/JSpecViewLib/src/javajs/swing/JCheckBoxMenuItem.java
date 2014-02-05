package javajs.swing;

public class JCheckBoxMenuItem extends JMenuItem {

  public JCheckBoxMenuItem() {
    super("chk");
  }

  
  @Override
  protected String htmlLabel() {
      return "<input id=\"ID-cb\" type=\"checkbox\" " + (this.selected ? "checked" : "") + " /><label for=\"ID-cb\">TeXt</label>";
  }

}

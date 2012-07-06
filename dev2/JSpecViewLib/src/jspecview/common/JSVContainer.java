package jspecview.common;

public interface JSVContainer {
  public void dispose();
  public boolean isEnabled();
  int getWidth();
  int getHeight();
  public String getTitle();
  public boolean isVisible();
  public boolean requestFocusInWindow();
  public void setEnabled(boolean b);
	public void setFocusable(boolean b);
	public boolean isFocusable();
  public void setTitle(String title);
}

package jspecview.common;

public interface JSVContainer {
  public String getTitle();
  public void setTitle(String title);
  public void dispose();
  public void setEnabled(boolean b);
  public boolean isEnabled();
  int getWidth();
  int getHeight();
  public boolean isVisible();
}

package jspecview.api;

public interface JSVViewPanel {
  void dispose();
  boolean isEnabled();
  int getWidth();
  int getHeight();
  String getTitle();
  boolean isVisible();
  void setEnabled(boolean b);
	void setFocusable(boolean b);
	boolean isFocusable();
  void setTitle(String title);
}

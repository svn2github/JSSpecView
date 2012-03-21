package jspecview.common;

public class SubSpecChangeEvent {

  private int isub;
  private String title;
  public SubSpecChangeEvent(int isub, String title) {
    this.isub = isub;
  }
  
  public boolean isValid() {
    return (title != null);
  }
  
  public int getSubIndex() {
    return isub;
  }
  
  @Override
  public String toString() {
    return title;
  }

}

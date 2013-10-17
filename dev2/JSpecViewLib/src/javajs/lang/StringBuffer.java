
package javajs.lang;

/**
 * Interesting thing here is that JavaScript is 3x faster than Java in handling strings.
 * 
 * Java StringBuilder is final, unfortunately. I guess they weren't thinking about Java2Script!
 * 
 * The reason we have to do this that several overloaded append methods is WAY too expensive
 * 
 */

public class StringBuffer {
  
  private StringBuilder sb;
  String s;
  
  //TODO: JS experiment with using array and .push() here

  public StringBuffer() {
    /**
     * @j2sNative
     * 
     *            this.s = "";
     * 
     */
    {
      sb = new StringBuilder();
    }
  }

  public static StringBuffer newN(int n) {
    /**
     * @j2sNative
     *            return new org.jmol.util.SB(); 
     */
    {
      // not perfect, because it requires defining sb twice. 
      // We can do better...
      StringBuffer sb = new StringBuffer();
      sb.sb = new StringBuilder(n);
      return sb;
    }
  }

  public static StringBuffer newS(String s) {
    /**
     * @j2sNative 
     * 
     * var sb = new org.jmol.util.SB();
     * sb.s = s;
     * return sb; 
     * 
     */
    {
    StringBuffer sb = new StringBuffer();
    sb.sb = new StringBuilder(s);
    return sb;
    }
  }

  public StringBuffer append(String s) {
    /**
     * @j2sNative
     * 
     *            this.s += s
     * 
     */
    {
      sb.append(s);
    }
    return this;
  }
  
  public StringBuffer appendC(char c) {
    /**
     * @j2sNative
     * 
     *            this.s += c;
     */
    {
      sb.append(c);
    }
    return this;
    
  }

  public StringBuffer appendI(int i) {
    /**
     * @j2sNative
     * 
     *            this.s += i
     * 
     */
    {
      sb.append(i);
    }
    return this;
  }

  public StringBuffer appendB(boolean b) {
    /**
     * @j2sNative
     * 
     *            this.s += b
     * 
     */
    {
      sb.append(b);
    }
    return this;
  }

  /**
   * note that JavaScript could drop off the ".0" in "1.0"
   * @param f
   * @return this
   */
  public StringBuffer appendF(float f) {
    /**
     * @j2sNative
     * 
     * var sf = "" + f;
     * if (sf.indexOf(".") < 0 && sf.indexOf("e") < 0)
     *   sf += ".0" ;
     *            this.s += sf;
     * 
     */
    {
      sb.append(f);
    }
    return this;
  }

  public StringBuffer appendD(double d) {
    /**
     * @j2sNative
     * 
     * var sf = "" + d;
     * if (sf.indexOf(".") < 0 && sf.indexOf("e") < 0)
     *   sf += ".0" ;
     *            this.s += sf;
     * 
     */
    {
      sb.append(d);
    }
    return this;
  }

  public StringBuffer appendSB(StringBuffer buf) {
    /**
     * @j2sNative
     * 
     *            this.s += buf.s;
     * 
     */
    {
      sb.append(buf.sb);
    }
    return this;
  }

  public StringBuffer appendO(Object data) {
    /**
     * @j2sNative
     * 
     *            this.s += data.toString();
     * 
     */
    {
      sb.append(data);
    }
    return this;
  }

  public void appendCB(char[] cb, int off, int len) {
    /**
     * @j2sNative
     * 
     * for (var i = len,j=off; --i >= 0;)
     *            this.s += cb[j++];
     * 
     */
    {
       sb.append(cb, off, len);
    }
  }

  @Override
  public String toString() {
    /**
     * @j2sNative
     * 
     *            return this.s;
     * 
     */
    {
      return sb.toString();
    }
  }

  public int length() {
    /**
     * @j2sNative
     * 
     *            return this.s.length;
     * 
     */
    {
      return sb.length();
    }
  }

  public int indexOf(String s) {
    /**
     * @j2sNative
     * 
     *            return this.s.indexOf(s);
     * 
     */
    {
      return sb.indexOf(s);
    }
  }

  public char charAt(int i) {
    /**
     * @j2sNative
     * 
     *            return this.s.charAt(i);
     * 
     */
    {
      return sb.charAt(i);
    }
  }

  public void setLength(int n) {
    /**
     * @j2sNative
     * 
     *            this.s = this.s.substring(0, n);
     */
    {
      sb.setLength(n);
    }
  }

  public int lastIndexOf(String s) {
    /**
     * @j2sNative
     * 
     *            return this.s.lastIndexOf(s);
     */
    {
      return sb.lastIndexOf(s);
    }
  }

  public int indexOf2(String s, int i) {
    /**
     * @j2sNative
     * 
     *            return this.s.indexOf(s, i);
     */
    {
      return sb.lastIndexOf(s, i);
    }
  }

  public String substring(int i) {
    /**
     * @j2sNative
     * 
     *            return this.s.substring(i);
     */
    {
      return sb.substring(i);
    }
  }

  public String substring2(int i, int j) {
    /**
     * @j2sNative
     * 
     *            return this.s.substring(i, j);
     */
    {
      return sb.substring(i, j);
    }
  }

  /**
   * simple byte conversion not allowing for unicode.
   * Used for base64 conversion and allows for offset
   * @param off 
   * @param len or -1 for full length (then off must = 0)
   * @return byte[]
   */
  public byte[] toBytes(int off, int len) {
    if (len < 0)
      len = length() - off;
    byte[] b = new byte[len];
    for (int i = off + len, j = i - off; --i >= off;)
      b[--j] = (byte) charAt(i);
    return b;
  }

}
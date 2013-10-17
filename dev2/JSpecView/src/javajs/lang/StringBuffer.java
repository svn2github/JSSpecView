
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
  
  private java.lang.StringBuilder StringBuffer;
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
      StringBuffer = new java.lang.StringBuilder();
    }
  }

  public static StringBuffer newN(int n) {
    /**
     * @j2sNative
     *            return new org.jmol.util.StringBuffer(); 
     */
    {
      // not perfect, because it requires defining StringBuffer twice. 
      // We can do better...
      StringBuffer StringBuffer = new StringBuffer();
      StringBuffer.StringBuffer = new java.lang.StringBuilder(n);
      return StringBuffer;
    }
  }

  public static StringBuffer newS(String s) {
    /**
     * @j2sNative 
     * 
     * var StringBuffer = new org.jmol.util.StringBuffer();
     * StringBuffer.s = s;
     * return StringBuffer; 
     * 
     */
    {
    StringBuffer StringBuffer = new StringBuffer();
    StringBuffer.StringBuffer = new java.lang.StringBuilder(s);
    return StringBuffer;
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
      StringBuffer.append(s);
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
      StringBuffer.append(c);
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
      StringBuffer.append(i);
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
      StringBuffer.append(b);
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
      StringBuffer.append(f);
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
      StringBuffer.append(d);
    }
    return this;
  }

  public StringBuffer appendStringBuffer(StringBuffer buf) {
    /**
     * @j2sNative
     * 
     *            this.s += buf.s;
     * 
     */
    {
      StringBuffer.append(buf.StringBuffer);
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
      StringBuffer.append(data);
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
       StringBuffer.append(cb, off, len);
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
      return StringBuffer.toString();
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
      return StringBuffer.length();
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
      return StringBuffer.indexOf(s);
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
      return StringBuffer.charAt(i);
    }
  }

  public void setLength(int n) {
    /**
     * @j2sNative
     * 
     *            this.s = this.s.substring(0, n);
     */
    {
      StringBuffer.setLength(n);
    }
  }

  public int lastIndexOf(String s) {
    /**
     * @j2sNative
     * 
     *            return this.s.lastIndexOf(s);
     */
    {
      return StringBuffer.lastIndexOf(s);
    }
  }

  public int indexOf2(String s, int i) {
    /**
     * @j2sNative
     * 
     *            return this.s.indexOf(s, i);
     */
    {
      return StringBuffer.lastIndexOf(s, i);
    }
  }

  public String substring(int i) {
    /**
     * @j2sNative
     * 
     *            return this.s.substring(i);
     */
    {
      return StringBuffer.substring(i);
    }
  }

  public String substring2(int i, int j) {
    /**
     * @j2sNative
     * 
     *            return this.s.substring(i, j);
     */
    {
      return StringBuffer.substring(i, j);
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

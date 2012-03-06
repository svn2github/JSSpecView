/* Copyright (c) 2002-2009 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;

import jspecview.util.Logger;

/**
 * <code>JDXSourceStringTokenizer</code> breaks up the <code>JDXSource</code>
 * into pairs of Label Data Records (LDRs).
 * When nextToken() is called, the label and value variables are updated.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.source.JDXSource
 */
public class JDXSourceStreamTokenizer {

  private BufferedReader br;
  
  JDXSourceStreamTokenizer(BufferedReader br) {
    this.br = br;
  }

  /**
   * The Label part of the next token
   */
  private String label;
  /**
   * The value part of the next token
   */
  private String value;

  /**
   * The line number of the label
   */
  private int labelLineNo = 0;
  private String line;

  private int lineNo;

  String getLabel() {
    label = null;
    value = null;
    while (line == null) {
      try {
        readLine();
        line = line.trim();
      } catch (IOException e) {
        line = "";
        return null;
      }
      System.out.println(line);
      if (line.startsWith("##"))
        break;
      line = null;
    }
    int pt = line.indexOf("=");
    if (pt < 0)
      return null;
    label = line.substring(0, pt).trim();
    line = line.substring(pt + 1);
    labelLineNo = lineNo;
    if (Logger.debugging)
      System.out.println(label);
    return cleanLabel(label);
  }
  
  /**
   * Extracts spaces, underscores etc. from the label
   * 
   * @param label
   *        the label to be cleaned
   * @return the new label
   */
  static String cleanLabel(String label) {
    if (label == null)
      return null;
    int i;
    StringBuffer str = new StringBuffer();

    for (i = 0; i < label.length(); i++) {
      switch (label.charAt(i)) {
      case '/':
      case '\\':
      case ' ':
      case '-':
      case '_':
        break;
      default:
        str.append(label.charAt(i));
        break;
      }
    }
    return str.toString().toUpperCase();
  }

  String getRawLabel() {
    return label;
  }

  int getLabelLineNo() {
    return labelLineNo;
  }

  public String getValue() {
    if (value != null)
      return value;
    StringBuffer sb = new StringBuffer(line);
    if (sb.length() > 0)
      sb.append('\n');
    try {
      while (readLine() != null) {
        if (line.indexOf("##") >= 0 && line.trim().startsWith("##"))
          break;
        sb.append(line).append('\n');
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    value = trimLines(sb);
    if (Logger.debugging)
      System.out.println(value);
    return value;
  }

  public String readLineTrimmed() throws IOException {
    readLine();
    if (line == null)
      return null;
    if (line.indexOf("$$") < 0)
      return line.trim();
    StringBuffer sb = new StringBuffer(line);
    return trimLines(sb).trim();
  }

  String flushLine() {
    StringBuffer sb = new StringBuffer(line);
    line = null;
    return trimLines(sb).trim();
  }

  private String readLine() throws IOException {
    line = br.readLine();
    lineNo++;
    return line;
  }

  private static String trimLines(StringBuffer v) {
    int n = v.length();
    int ilast = n - 1;
    int vpt = ptNonWhite(v, 0, n);
    // no line trimming for XML or <....> data
    if (vpt >= n)
      return "";
    if (v.charAt(vpt) == '<') {
      n = v.lastIndexOf(">");
      if (n < 0)
        n = v.length();
      return v.substring(vpt, n);
    }
    char[] buffer = new char[n - vpt];
    int pt = 0;
    for (;vpt < n; vpt++) {
      char ch;
      switch (ch = v.charAt(vpt)) {
      case '\r':
        if (vpt < ilast && v.charAt(vpt + 1) == '\n')
          continue;
        ch = '\n';
        break;
      case '\n':
        if (pt > 0 && buffer[pt - 1] != '\n')
          pt -= vpt - ptNonSpaceRev(v, vpt) - 1;
        vpt = ptNonSpace(v, ++vpt, n) - 1;
        break;
      case '$':
        if (vpt < ilast && v.charAt(vpt + 1) == '$') {
          vpt++;
          while (++vpt < n && "\n\r".indexOf(v.charAt(vpt)) < 0) {
            // skip to end of line
          }
          continue;
        }
        break;
      }
      if (ch == '\n' && pt > 0 && buffer[pt - 1] == '\n')
        continue;
      buffer[pt++] = ch;
    }
    if (buffer[pt - 1] == '\n')
      --pt;
    return (new String(buffer)).substring(0, pt);
  }

  private static int ptNonWhite(StringBuffer v, int pt, int n) {
    while (pt < n && Character.isWhitespace(v.charAt(pt))) 
      pt++;
    return pt;
  }

  private static int ptNonSpace(StringBuffer v, int pt, int n) {
    while (pt < n && (v.charAt(pt) == ' ' || v.charAt(pt) == '\t'))
      pt++;
    return pt;
  }

  private static int ptNonSpaceRev(StringBuffer v, int pt) {
    while (--pt >= 0 && (v.charAt(pt) == ' ' || v.charAt(pt) == '\t')) {
      // move on back one character
    }
    return pt;
  }


}

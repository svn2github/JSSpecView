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

import java.util.StringTokenizer;

/**
 * <code>JDXSourceStringTokenizer</code> breaks up the <code>JDXSource</code>
 * into pairs of Label Data Records (LDRs).
 * When nextToken() is called, the label and value variables are updated.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.source.JDXSource
 */
class JDXSourceStringTokenizer extends StringTokenizer {

  public JDXSourceStringTokenizer(String str) {
    super(str);
  }

  /**
   * The Label part of the next token
   */
  String label;
  /**
   * The value part of the next token
   */
  String value;

  /**
   * The line number of the label
   */
  int labelLineNo = 0;

  /**
   * variable to keep a count of the number of lines that a dataset takes up
   */
  private int dataSetLineCount = 1;
  
  /**
   * Gets the next token from the string and stores the label and the value
   */
  boolean getNextToken() {
    // ADD CODE TO IGNORE ##= COMMENTS
    // TWO LDR'S CAN'T BE ON THE SAME LINE

    labelLineNo += dataSetLineCount;

    label = nextToken("=");
    StringBuffer v = new StringBuffer(nextToken("##"));

    // ## At the start of a line, preceded only by blanks indicate the
    // start of a data-label.
    
    int pt = 0;
    while (hasMoreTokens() && (pt = ptNonSpaceRev(v, v.length())) >= 0
        && "\n\r".indexOf(v.charAt(pt)) < 0) {
      //  ## only counts if at start of line
      v.append("##").append(nextToken("##"));
    }

    // count lines
    StringBuffer dataSet = new StringBuffer(label);
    dataSet.append(v);
    char c = (dataSet.indexOf("\r") < 0 ? '\n' : '\r');
    dataSetLineCount = 0;
    for (int i = dataSet.length(); --i >= 0;)
      if (dataSet.charAt(i) == c)
        dataSetLineCount++;
    
    // fix up label and value
    label = label.trim();
    value = trimLines(v);
    if (label.equals("##TITLE") || label.equals("##END"))
      System.out.println(label + "\t" + value);
    return true;
  }

  private String trimLines(StringBuffer v) {
    int n = v.length();
    int ilast = n - 1;
    int vpt = ptNonWhite(v, 1, n);
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
          while (vpt < ilast && "\n\r".indexOf(v.charAt(++vpt)) < 0) {
            // skip to end of line
          }
          vpt--;
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

//    BufferedReader lineReader = new BufferedReader(new StringReader(v));
//    String line;
//    try {
//      while ((line = lineReader.readLine()) != null){
//        line = line.trim();
//        int commentIndex = line.indexOf("$$");
//        // ignore comments that start at the beginning of the line
//        // or empty lines
//        if(commentIndex == 0)
//          continue;
//
//        // remove comments from the end of a line
//        if(commentIndex != -1)
//          line = line.substring(0, commentIndex).trim();
//        valueBuffer.append(line).append(TextFormat.newLine);
//      }
//    }
}

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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package jspecview.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;

import jspecview.util.TextFormat;


/**
 * <code>JDXSourceStringTokenizer</code> breaks up the <code>JDXSource</code>
 * into pairs of Label Data Records (LDRs).
 * When nextToken() is called, the label and value variables are updated.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.source.JDXSource
 */
public class JDXSourceStringTokenizer {
  /**
   * The Label part of the next token
   */
  public String label;
  /**
   * The value part of the next token
   */
  public String value;

  /**
   * The StringTokenizer used to return tokens
   */
  private StringTokenizer st;

  /**
   * The line number of the label
   */
  public int labelLineNo = 0;

  /**
   * variable to keep a count of the number of lines that a dataset takes up
   */
  private int dataSetLineCount = 1;

  /**
   * Constructor creates a new JDXSourceStringTokenizer from a string
   * @param contents the source string
   */
  public JDXSourceStringTokenizer(String contents){
    st = new StringTokenizer(contents);
  }

  private StringBuffer dataSet = new StringBuffer();

  /**
   * Gets the next token from the string and stores the label and the value
   */
  public boolean nextToken() {
    // ADD CODE TO IGNORE ##= COMMENTS
    // TWO LDR'S CAN'T BE ON THE SAME LINE

    labelLineNo += dataSetLineCount;

    // recontruct dataset
    String l = st.nextToken("=");
    String v = st.nextToken("##");
    // TODO:  Not testing here for NON jdx format?
    dataSet.setLength(0);
    dataSet.append(l).append(v).append(TextFormat.newLine);
    StringReader stringReader = new StringReader(dataSet.toString());
    BufferedReader lineReader = new BufferedReader(stringReader);

    // find out how many lines the dataset takes up
    dataSetLineCount = -1;
    try {
      while (lineReader.readLine() != null)
        dataSetLineCount++;
    } catch (IOException ex) {
    }
    label = l.trim();
    value = (v.startsWith("<") && v.contains("</") ? v : trimComments(v));
    value = value.substring(value.indexOf("=") + 1).trim();
    if (label.equals("##TITLE") || label.equals("##END"))
      System.out.println(label + "\t" + value);
    return true;
  }

  private String trimComments(String v) {
    
    // trim comments
    StringBuffer valueBuffer = new StringBuffer();
    BufferedReader lineReader = new BufferedReader(new StringReader(v));
    String line;
    try {
      while ((line = lineReader.readLine()) != null){
        line = line.trim();
        int commentIndex = line.indexOf("$$");
        // ignore comments that start at the beginning of the line
        // or empty lines
        if(commentIndex == 0)
          continue;

        // remove comments from the end of a line
        if(commentIndex != -1)
          line = line.substring(0, commentIndex).trim();
        valueBuffer.append(line).append(TextFormat.newLine);
      }
    }
    catch (IOException ex) {
    }
    return valueBuffer.toString().trim();
  }

  /**
   * Returns true if the source string has more tokens
   * @return true if the source string has more tokens other false
   */
  public boolean hasMoreTokens(){
    return st.hasMoreTokens();
  }
}

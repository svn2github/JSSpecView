/* Copyright (c) 2002-2007 The University of the West Indies
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

package jspecview.util;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

/**
 * An alternate implementation of a tokenizer for a <code>JDXSource</code>.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.util.JDXSourceStringTokenizer
 */

public class JDXTokenizer {

  /**
   * The dataset label
   */
  public String label;

  /**
   * The data part of the LDR
   */
  public String data;

  /**
   * The line number
   */
  public int lineNo = 0;

  /**
   * Reader used to read lines of Source that will be parsed or tokenized
   */
  private LineNumberReader lineReader;

  /**
   * Initialises the <code>JDXTokenizer</code> with the contents of the
   * Source as a String
   * @param contents the contents of the JDXSource
   */
  public JDXTokenizer(String contents) {
    StringReader reader = new StringReader(contents);
    lineReader = new LineNumberReader(reader);
  }

  /**
   * Find the next LDR and updates the label and data variables
   * @return 1 if successful, -1 otherwise
   */
  public int nextLDR(){
    //REMOVE COMMENTS
    //DO NOT RETURN COMMENT LDR


    int equalIndex;
    StringBuffer dataBuffer = new StringBuffer();

    try {
      String line = lineReader.readLine();
      if(line != null){
        equalIndex = line.indexOf("=");
        if(equalIndex != -1){
          label = line.substring(0, equalIndex).trim();
          lineNo = lineReader.getLineNumber();
          dataBuffer.append(line.substring(equalIndex + 1) + JSpecViewUtils.newLine);
          lineReader.mark(255);
          line = lineReader.readLine();
          while(line != null){
            if(line.trim().indexOf("##") == 0){
              lineReader.reset();
              break;
            }
            dataBuffer.append(line + JSpecViewUtils.newLine);
            lineReader.mark(255);
            line = lineReader.readLine();  
          }
          data = dataBuffer.toString();
          return 1;
        }
      }
    }
    catch (IOException ex) {
    }

    return -1;
  }
}

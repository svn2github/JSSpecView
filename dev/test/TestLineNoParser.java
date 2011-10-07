/* Copyright (c) 2002-2006 The University of the West Indies
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

package test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import jspecview.source.JDXSourceStringTokenizer;
import jspecview.common.JSpecViewUtils;

// Adding Comment to test update from command line
// Testing another update...
// Here's to another test...using wincvs this time

/**
 * <p>Title: JSpecView</p>
 * <p>Description: A Graphical View for JCAMP-DX Files</p>
 * <p>Copyright: Copyright (c) 2002-2011</p>
 * <p>Company: Dept. of Chemistry, University of the West Indies, Jamaica</p>
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @version 1.0
 */

public class TestLineNoParser {

  public TestLineNoParser() {
  }

  private String getSourceContents(String fileName){
    StringBuffer contents = new StringBuffer();

    try
    {
      InputStream in = new FileInputStream(fileName);
      BufferedReader d = new BufferedReader(
        new InputStreamReader(in)
      );

      String line = null;

      while ((line = d.readLine()) != null){
        contents.append(line + JSpecViewUtils.newLine);
      }
    }
    catch (IOException ioe){
      return null;
    }

    return contents.toString();
  }


  public void parseFile(String fileName){
    String contents = getSourceContents(fileName);

    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(contents);
    while(t.hasMoreTokens()){
      t.nextToken();
      System.out.println(t.labelLineNo + ": " + t.label + " = " + t.value);
      System.out.println();
    }

    /*
    JDXTokenizer t = new JDXTokenizer(contents);
    while(t.nextLDR() != -1){
      System.out.println(t.lineNo + ": " + t.label + " - " + t.data);
      System.out.println();
    }
    */
  }

  public static void main(String[] args) {
    TestLineNoParser test = new TestLineNoParser();
    test.parseFile("../../testdata/NMR/h0019.jdx");
  }
}

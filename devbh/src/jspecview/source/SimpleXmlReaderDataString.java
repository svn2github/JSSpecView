/* Copyright (c) 2007 The University of the West Indies
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

class SimpleXmlReaderDataString {

  StringBuffer data;
  BufferedReader reader;
  int ptr;
  int ptEnd;

  SimpleXmlReaderDataString() {
    this.data = new StringBuffer();
  }

  SimpleXmlReaderDataString(StringBuffer data) {
    this.data = data;
    ptEnd = data.length();
  }

  String substring(int i) {
    return data.substring(i);
  }

  String substring(int i, int j) {
    return data.substring(i, j);
  }

  int skipOver(char c, boolean inQuotes) throws IOException {
    if (skipTo(c, inQuotes) > 0 && ptr != ptEnd)
      ptr++;
    return ptr;
  }

  int skipTo(char toWhat, boolean inQuotes) throws IOException {
    if (data == null)
      return -1;
    char ch;
    if (ptr == ptEnd) {
      if (reader == null)
        return -1;
      readLine();
    }
    int ptEnd1 = ptEnd - 1;
    while (ptr < ptEnd && (ch = data.charAt(ptr)) != toWhat) {
      if (inQuotes && ch == '\\' && ptr < ptEnd1) {
        // must escape \" by skipping the quote and 
        // must escape \\" by skipping the second \ 
        if ((ch = data.charAt(ptr + 1)) == '"' || ch == '\\')
          ptr++;
      } else if (ch == '"') {
        ptr++;
        if (skipTo('"', true) < 0)
          return -1;
      }
      if (++ptr == ptEnd) {
        if (reader == null)
          return -1;
        readLine();
      }
    }
    return ptr;
  }

  @SuppressWarnings("unused")
  protected boolean readLine() throws IOException {
    return false;
  }
}



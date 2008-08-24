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
import java.io.InputStreamReader;
import java.io.InputStream;

class SimpleXmlReader {

  /*
   * A simple very light-weight XML reader 
   * See AnIMLSource.java and CMLSource.java for implementation.
   * 
   *  Bob Hanson hansonr@stolaf.edu  8/22/2008 
   * 
   * 
   */
  
  SimpleXmlReader() {  
    System.out.println("SimpleXMLREADER");
  }
  
  Buffer getBuffer(InputStream in) {
    return new Buffer(in);
  }
  
  final static int TAG_NONE = 0;
  final static int START_ELEMENT = 1;
  final static int END_ELEMENT = 2;
  final static int START_END_ELEMENT = 3;
  final static int CHARACTERS = 4;
  final static int EOF = 8;

  private SimpleXmlReaderXmlEvent e = new SimpleXmlReaderXmlEvent(TAG_NONE);


  class Buffer extends SimpleXmlReaderDataString {

    Buffer(InputStream in) {
      reader = new BufferedReader(new InputStreamReader(in));
    }
    int nCharsRemaining;

    boolean hasNext() {
      try {
        readLine();
      } catch (IOException e) {
        return false;
      }
      return (nCharsRemaining != 0);
    }

    @Override
    protected boolean readLine() throws IOException {
      String s = reader.readLine();
      if (s == null) {
        nCharsRemaining = ptEnd - ptr;
        return false;
      }
      data.append(s + "\n");
      ptEnd = data.length();
      nCharsRemaining = ptEnd - ptr;
      return true;
    }

    int getNCharactersRemaining() {
      return nCharsRemaining = ptEnd - ptr;
    }
    
    private void flush() {
      data = new StringBuffer(data.substring(ptr));
      ptEnd = nCharsRemaining = data.length();
      ptr = 0;
    }

    SimpleXmlReaderXmlEvent peek() throws IOException {
      if (nCharsRemaining < 2)
        try {
          readLine();
        } catch (IOException e) {
          return new SimpleXmlReaderXmlEvent(EOF);
        }
      int pt0 = ptr;
      SimpleXmlReaderXmlEvent e = new SimpleXmlReaderXmlEvent(this);
      ptr = pt0;
      return e;
    }

    SimpleXmlReaderXmlEvent nextTag() throws IOException {
      flush();
      skipTo('<', false);
      SimpleXmlReaderXmlEvent e = new SimpleXmlReaderXmlEvent(this);
      return e;
    }

    SimpleXmlReaderXmlEvent nextEvent() throws IOException {
      flush();
      // cursor is always left after the last element
      return new SimpleXmlReaderXmlEvent(this);
    }

  }

  private Buffer fer;

  protected boolean haveMore() {
    return fer.hasNext();
  }

  protected void nextTag() throws IOException {
    e = fer.nextTag();
  }

  protected int nextEvent() throws IOException {
    e = fer.nextEvent();
    return e.getEventType();
  }

  protected void nextStartTag() throws IOException {
    e = fer.nextTag();
    while (!e.isStartElement())
      e = fer.nextTag();
  }

  protected String getTagName() {
    return e.getTagName();
  }

  protected String getEndTag() {
    return e.getTagName();
  }

  protected String thisValue() throws IOException {
    return fer.nextEvent().toString().trim();
  }

  protected String nextValue() throws IOException {
    fer.nextTag();
    return fer.nextEvent().toString().trim();
  }

  protected String getAttributeList() {
    return e.toString().toLowerCase();
  }

  protected String getAttrValueLC(String key) {
    return getAttrValue(key).toLowerCase();
  }

  protected SimpleXmlReaderAttribute getAttr(String name) {
    return e.getAttributeByName(name);
  }

  protected String getAttrValue(String name) {
    SimpleXmlReaderAttribute a = getAttr(name);
    return (a == null ? "" : a.getValue());
  }

  protected String getFullAttribute(String name) {
    SimpleXmlReaderAttribute a = getAttr(name);
    return (a == null ? "" : a.toString().toLowerCase());
  }
  
  protected String getCharacters() throws IOException {
    StringBuffer sb = new StringBuffer();
    e = fer.peek();
    int eventType = e.getEventType();

    while (eventType != CHARACTERS)
      e = fer.nextEvent();
    while (eventType == CHARACTERS) {
      e = fer.nextEvent();
      eventType = e.getEventType();
      if (eventType == CHARACTERS)
        sb.append(e.toString());
    }
    return sb.toString();
  }
}

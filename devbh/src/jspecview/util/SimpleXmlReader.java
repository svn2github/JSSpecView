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

package jspecview.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.Hashtable;

public class SimpleXmlReader {

  /*
   * A simple very light-weight XML reader 
   * See AnIMLSource.java and CMLSource.java for implementation.
   * 
   *  Bob Hanson hansonr@stolaf.edu  8/22/2008 
   * 
   * 
   */
  
  private XmlEvent thisEvent = new XmlEvent(TAG_NONE);
  private Buffer buffer;

  public final static int TAG_NONE = 0;
  public final static int START_ELEMENT = 1;
  public final static int END_ELEMENT = 2;
  public final static int START_END_ELEMENT = 3;
  public final static int CHARACTERS = 4;
  public final static int EOF = 8;


  public SimpleXmlReader(InputStream in) {
    buffer = new Buffer(in);
  }
  
  public String getBufferData() {
    return (buffer == null ? null : buffer.data.substring(0, buffer.ptr));
  }

  /**
   * for value without surrounding tag
   * 
   * @return value
   * @throws IOException
   */
  public String thisValue() throws IOException {
    return buffer.nextEvent().toString().trim();
  }

  /**
   * for &lt;xxxx&gt; value &lt;/xxxx&gt;
   * 
   * @return value
   * @throws IOException
   */
  public String qualifiedValue() throws IOException {
    buffer.nextTag();
    String value = buffer.nextEvent().toString().trim();
    buffer.nextTag();
    return value;
  }

  public int peek() throws IOException {
    thisEvent = buffer.peek();
    return thisEvent.getEventType();
  }

  public boolean hasNext() {
    return buffer.hasNext();
  }

  public void nextTag() throws IOException {
    thisEvent = buffer.nextTag();
  }

  public int nextEvent() throws IOException {
    thisEvent = buffer.nextEvent();
    return thisEvent.getEventType();
  }

  public void nextStartTag() throws IOException {
    thisEvent = buffer.nextTag();
    while (!thisEvent.isStartElement())
      thisEvent = buffer.nextTag();
  }

  public String getTagName() {
    return thisEvent.getTagName();
  }

  public int getTagType() {
    return thisEvent.getTagType();
  }

  public String getEndTag() {
    return thisEvent.getTagName();
  }

  public String nextValue() throws IOException {
    buffer.nextTag();
    return buffer.nextEvent().toString().trim();
  }

  public String getAttributeList() {
    return thisEvent.toString().toLowerCase();
  }

  public String getAttrValueLC(String key) {
    return getAttrValue(key).toLowerCase();
  }

  public Attribute getAttr(String name) {
    return thisEvent.getAttributeByName(name);
  }

  public String getAttrValue(String name) {
    Attribute a = getAttr(name);
    return (a == null ? "" : a.getValue());
  }

  public String getFullAttribute(String name) {
    Attribute a = getAttr(name);
    return (a == null ? "" : a.toString().toLowerCase());
  }
  
  public String getCharacters() throws IOException {
    StringBuffer sb = new StringBuffer();
    thisEvent = buffer.peek();
    int eventType = thisEvent.getEventType();

    while (eventType != CHARACTERS)
      thisEvent = buffer.nextEvent();
    while (eventType == CHARACTERS) {
      thisEvent = buffer.nextEvent();
      eventType = thisEvent.getEventType();
      if (eventType == CHARACTERS)
        sb.append(thisEvent.toString());
    }
    return sb.toString();
  }
  

  class Buffer extends DataString {

    Buffer(InputStream in) {
      reader = new BufferedReader(new InputStreamReader(in));
    }

    boolean hasNext() {
      if (ptr == ptEnd)
        try {
          readLine();
        } catch (IOException e) {
          return false;
        }
      return ptr < ptEnd;
    }

    @Override
    public boolean readLine() throws IOException {
      String s = reader.readLine();
      if (s == null) {
        return false;
      }
      data.append(s + "\n");
      ptEnd = data.length();
      return true;
    }

    XmlEvent peek() throws IOException {
      if (ptEnd - ptr < 2)
        try {
          readLine();
        } catch (IOException e) {
          return new XmlEvent(EOF);
        }
      int pt0 = ptr;
      XmlEvent e = new XmlEvent(this);
      ptr = pt0;
      return e;
    }

    XmlEvent nextTag() throws IOException {
      flush();
      skipTo('<', false);
      XmlEvent e = new XmlEvent(this);
      return e;
    }

    XmlEvent nextEvent() throws IOException {
      flush();
      // cursor is always left after the last element
      return new XmlEvent(this);
    }

  }

  class DataString {

    StringBuffer data;
    protected BufferedReader reader;
    int ptr;
    int ptEnd;

    DataString() {
      this.data = new StringBuffer();
    }

    DataString(StringBuffer data) {
      this.data = data;
      ptEnd = data.length();
    }

    int getNCharactersRemaining() {
      return ptEnd - ptr;
    }
    
    protected void flush() {
      if (data.length() < 1000 || ptEnd - ptr > 100)
        return;
      data = new StringBuffer(data.substring(ptr));
      //System.out.println(data);
      ptr = 0;
      ptEnd = data.length();
      System.out.println("flush " + ptEnd);
    }

    String substring(int i, int j) {
      return data.substring(i, j);
    }

    int skipOver(char c, boolean inQuotes) throws IOException {
      if (skipTo(c, inQuotes) > 0 && ptr != ptEnd) {
        ptr++;
      }
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
    public boolean readLine() throws IOException {
      return false;
    }
  }

  class XmlEvent {

    int eventType = TAG_NONE;
    int ptr = 0;
    Tag tag;
    String data;

    @Override
    public String toString() {
      return (data != null ? data : tag != null ? tag.text : null);
    }

    XmlEvent(int eventType) {
      this.eventType = eventType;
    }

    XmlEvent(Buffer b) throws IOException {
      ptr = b.ptr;
      int n = b.getNCharactersRemaining();
      eventType = (n == 0 ? EOF : n == 1
          || b.data.charAt(b.ptr) != '<' ? CHARACTERS
          : b.data.charAt(b.ptr + 1) != '/' ? START_ELEMENT : END_ELEMENT);
      if (eventType == EOF)
        return;
      if (eventType == CHARACTERS) {
        b.skipTo('<', false);
        data = b.data.toString().substring(ptr, b.ptr);
      } else {
        b.skipOver('>', false);
        String s = b.data.substring(ptr, b.ptr);
        //System.out.println("new tag: " + s);
        tag = new Tag(s);
      }
    }

    public int getEventType() {
      return eventType;
    }

    boolean isEndElement() {
      return (eventType & END_ELEMENT) != 0;
    }

    boolean isStartElement() {
      return (eventType & START_ELEMENT) != 0;
    }

    public String getTagName() {
      return (tag == null ? null : tag.getName());
    }

    public int getTagType() {
      return (tag == null ? TAG_NONE : tag.tagType);
    }

    public String getFullAttribute(String name) {
      Attribute a = getAttributeByName(name);
      return (a == null ? "" : a.toString().toLowerCase());
    }

    public String getAttrValue(String name) {
      Attribute a = getAttributeByName(name);
      return (a == null ? "" : a.getValue());
    }    

    public Attribute getAttributeByName(String name) {
      return (tag == null ? null : tag.getAttributeByName(name));
    }

}
  
  class Tag {
    int tagType;
    String name;
    String text;
    private Hashtable<String, Object> attributes;

    Tag() {
      System.out.println("tag");
    }

    Tag(String fulltag) {
      text = fulltag;
      tagType = (fulltag.charAt(1) == '/' ? END_ELEMENT : fulltag
          .charAt(fulltag.length() - 2) == '/' ? START_END_ELEMENT
          : START_ELEMENT);
    }

    String getName() {
      if (name != null)
        return name;
      int ptTemp = (tagType == END_ELEMENT ? 2 : 1);
      int n = text.length() - (tagType == START_END_ELEMENT ? 2 : 1);
      while (ptTemp < n && Character.isWhitespace(text.charAt(ptTemp)))
        ptTemp++;
      int pt0 = ptTemp;
      while (ptTemp < n && !Character.isWhitespace(text.charAt(ptTemp)))
        ptTemp++;
      return name = text.substring(pt0, ptTemp).toLowerCase().trim();
    }

    Attribute getAttributeByName(String attrName) {
      if (attributes == null)
        getAttributes();
      return (Attribute) attributes.get(attrName.toLowerCase());
    }

    private void getAttributes() {
      attributes = new Hashtable<String, Object>();
      DataString d = new DataString(
          new StringBuffer(text));
      try {
        if (d.skipTo(' ', false) < 0)
          return;
        int pt0;
        while ((pt0 = ++d.ptr) >= 0) {
          if (d.skipTo('=', false) < 0)
            return;
          String name = d.substring(pt0, d.ptr).trim().toLowerCase();
          d.skipTo('"', false);
          pt0 = ++d.ptr;
          d.skipTo('"', true);
          String attr = d.substring(pt0, d.ptr);
          attributes.put(name, new Attribute(name, attr));
          int pt1 = name.indexOf(":");
          if (pt1 >= 0) {
            name = name.substring(pt1).trim();
            attributes.put(name, new Attribute(name, attr));
          }

        }
      } catch (IOException e) {
        // not relavent
      }
    }

  }


  class Attribute {
    String name;
    String value;

    Attribute(String name, String value) {
      this.name = name;
      this.value = value;
    }

    String getName() {
      return name;
    }

    String getValue() {
      return value;
    }
  }
}

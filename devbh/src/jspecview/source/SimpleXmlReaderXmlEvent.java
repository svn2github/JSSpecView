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

import java.io.IOException;

class SimpleXmlReaderXmlEvent {

  int eventType = SimpleXmlReader.TAG_NONE;
  int ptr = 0;
  SimpleXmlReaderTag tag;
  String data;

  @Override
  public String toString() {
    return (data != null ? data : tag != null ? tag.text : null);
  }

  SimpleXmlReaderXmlEvent(int eventType) {
    this.eventType = eventType;
  }

  SimpleXmlReaderXmlEvent(SimpleXmlReader.Buffer b) throws IOException {
    ptr = b.ptr;
    eventType = (b.getNCharactersRemaining() == 0 ? SimpleXmlReader.EOF : b.nCharsRemaining == 1
        || b.data.charAt(b.ptr) != '<' ? SimpleXmlReader.CHARACTERS
        : b.data.charAt(b.ptr + 1) != '/' ? SimpleXmlReader.START_ELEMENT : SimpleXmlReader.END_ELEMENT);
    if (eventType == SimpleXmlReader.EOF)
      return;
    if (eventType == SimpleXmlReader.CHARACTERS) {
      b.skipTo('<', false);
      this.data = b.data.toString().substring(ptr, b.ptr);
    } else {
      b.skipOver('>', false);
      String s = b.data.substring(ptr, b.ptr);
      b.getNCharactersRemaining();
      //System.out.println("new tag: " + s);
      tag = new SimpleXmlReaderTag(s);
    }
  }

  public int getEventType() {
    return eventType;
  }

  boolean isEndElement() {
    return (eventType & SimpleXmlReader.END_ELEMENT) != 0;
  }

  boolean isStartElement() {
    return (eventType & SimpleXmlReader.START_ELEMENT) != 0;
  }

  public String getTagName() {
    return (tag == null ? null : tag.getName());
  }

  public SimpleXmlReaderAttribute getAttributeByName(String name) {
    return (tag == null ? null : tag.getAttributeByName(name));
  }
}


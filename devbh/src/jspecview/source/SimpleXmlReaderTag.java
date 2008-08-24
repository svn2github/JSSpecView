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
import java.util.Hashtable;

class SimpleXmlReaderTag {
  int tagType;
  String name;
  String text;
  private Hashtable<String, Object> attributes;

  SimpleXmlReaderTag() {
    System.out.println("simplexmlreadertag");
  }

  SimpleXmlReaderTag(String fulltag) {
    text = fulltag;
    tagType = (fulltag.charAt(1) == '/' ? SimpleXmlReader.END_ELEMENT : fulltag
        .charAt(fulltag.length() - 2) == '/' ? SimpleXmlReader.START_END_ELEMENT
        : SimpleXmlReader.START_ELEMENT);
  }

  String getName() {
    if (name != null)
      return name;
    int ptTemp = (tagType == SimpleXmlReader.END_ELEMENT ? 2 : 1);
    int n = text.length() - (tagType == SimpleXmlReader.START_END_ELEMENT ? 2 : 1);
    while (ptTemp < n && Character.isWhitespace(text.charAt(ptTemp)))
      ptTemp++;
    int pt0 = ptTemp;
    while (ptTemp < n && !Character.isWhitespace(text.charAt(ptTemp)))
      ptTemp++;
    return name = text.substring(pt0, ptTemp).toLowerCase().trim();
  }

  SimpleXmlReaderAttribute getAttributeByName(String attrName) {
    if (attributes == null)
      getAttributes();
    return (SimpleXmlReaderAttribute) attributes.get(attrName.toLowerCase());
  }

  private void getAttributes() {
    attributes = new Hashtable<String, Object>();
    SimpleXmlReaderDataString d = new SimpleXmlReaderDataString(
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
        attributes.put(name, new SimpleXmlReaderAttribute(name, attr));
        int pt1 = name.indexOf(":");
        if (pt1 >= 0) {
          name = name.substring(pt1).trim();
          attributes.put(name, new SimpleXmlReaderAttribute(name, attr));
        }

      }
    } catch (IOException e) {
      // not relavent
    }
  }

}


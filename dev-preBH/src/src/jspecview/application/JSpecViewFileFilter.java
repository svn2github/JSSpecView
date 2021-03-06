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

package jspecview.application;

import java.io.File;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;

/**
 * A implementation of the <code>FileFilter</code> that filters out all files
 * except those with extensions that are added.
 * @see jspecview.application.JSpecViewFileFilter#JSpecViewFileFilter(java.lang.String[])
 * @see jspecview.application.JSpecViewFileFilter#JSpecViewFileFilter(java.lang.String[], java.lang.String)
 * @see jspecview.application.JSpecViewFileFilter#addExtension(java.lang.String)
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class JSpecViewFileFilter extends FileFilter
{

  /**
   * A list of file extensions
   */
  private Hashtable<String,Object> filters = null;

  /**
   * Description of the filter
   */
  private String description = null;

  /**
   * Intialises a <code>JSpecViewFileFilter</code>
   */
  public JSpecViewFileFilter() {
    this.filters = new Hashtable<String,Object>();
  }

  /**
   * Initialises the <code>JSpecViewFileFilter</code> with an list of extensions
   * to filter
   * @param filters a <code>String</code> of filters
   */
  public JSpecViewFileFilter(String[] filters) {
    this(filters, null);
  }

  /**
   * Initialises the <code>JSpecViewFileFilter</code> with an list of extensions
   * to filter and a description of the filter
   * @param filters the array of file extensions
   * @param description the description
   */
  public JSpecViewFileFilter(String[] filters, String description) {
    this();
    for (int i = 0; i < filters.length; i++) {
	    addExtension(filters[i]);
    }
    setDescription(description);
  }

  /**
   * Implementation of method from interface <code>FileFilter</code>.
   * @param f the the file to be filtered
   * @return true if the file should be shown, otherwise false
   */
  public boolean accept(File f) {
	if(f != null) {
	    if(f.isDirectory()) {
		return true;
	    }
	    String extension = getExtension(f);
	    if(extension != null && filters.get(getExtension(f)) != null) {
		return true;
	    }
	}
	return false;
  }

  /**
   * Returns the extension of a file
   * @param f the file
   * @return the extension of a file
   */
  public String getExtension(File f) {
    if(f != null) {
	    String filename = f.getName();
	    int i = filename.lastIndexOf('.');
	    if(i>0 && i<filename.length()-1) {
		return filename.substring(i+1).toLowerCase();
	    };
	}
	return null;
  }

  /**
   * Adds an extension to the </code>JSpecViewFileFilter</code>
   * @param extension the extension
   */
  public void addExtension(String extension) {
	if(filters == null) {
	    filters = new Hashtable<String,Object>(5);
	}
	filters.put(extension.toLowerCase(), this);
  }

  /**
   * Returns the </code>JSpecViewFileFilter</code> description
   * @return the </code>JSpecViewFileFilter</code> description
   */
  public String getDescription() {
	return description;
  }

  /**
   * Sets the description for the </code>JSpecViewFileFilter</code>
   * @param description the description
   */
  public void setDescription(String description) {
	this.description = description;
  }

}

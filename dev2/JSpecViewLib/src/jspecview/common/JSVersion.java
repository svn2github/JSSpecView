/*
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
		

package jspecview.common;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javajs.J2SRequireImport;

import org.jmol.util.Txt;


@J2SRequireImport({java.util.Properties.class, java.io.BufferedInputStream.class})
public class JSVersion {

  public static final String VERSION;
	public static final String VERSION_SHORT;

	static {
		String tmpVersion = null;
		String tmpDate = null;
		String tmpSVN = null;
		Properties props = new Properties();

		// Reading version from resource inside jar
		BufferedInputStream bis = null;
		InputStream is = null;
		String s = "jspecview/common/TODO.txt"; 
		try {
			/**
			 * @j2sNative
			 * 
			 * s = "JSV/common/TODO.txt";
			 * 
			 */
			{
				
			}
			is = JSVersion.class.getClassLoader().getResourceAsStream(s);
					
			bis = new BufferedInputStream(is);
			props.load(bis);
			tmpVersion = Txt.trimQuotes(props.getProperty("___version", tmpVersion));
			tmpDate = Txt.trimQuotes(props.getProperty("___date", tmpDate));
			tmpSVN = Txt.trimQuotes(props.getProperty("___svnRev", tmpSVN));
			if (tmpDate != null)
				tmpDate = tmpDate.substring(7, 23);
			if (tmpSVN != null)
				tmpSVN = tmpSVN.substring(22,27);
		} catch (IOException e) {
			// Nothing to do
		} finally {
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException e) {
					// Nothing to do
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// Nothing to do
				}
			}
		}
		VERSION_SHORT = (tmpVersion != null ? tmpVersion : "(Unknown version)");
		VERSION = VERSION_SHORT + "/SVN" + tmpSVN + "/" + (tmpDate != null ? tmpDate : "(Unknown date)");
  }

  
}

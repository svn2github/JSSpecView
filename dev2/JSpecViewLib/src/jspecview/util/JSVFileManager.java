/* Copyright (c) 2002-2008 The University of the West Indies
 *
 * Contact: robert.lancashire@uwimona.edu.jm
 * Author: Bob Hanson (hansonr@stolaf.edu) and Jmol developers -- 2008
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import jspecview.common.JSVersion;

public class JSVFileManager {

	// ALL STATIC METHODS
	
	public static String SIMULATION_PROTOCOL = "http://SIMULATION/";

	/**
	 * @param name 
	 * @param appletDocumentBase
	 * @return file as string
	 * 
	 */

	public static String getFileAsString(String name, URL appletDocumentBase) {
		if (name == null)
			return null;
		BufferedReader br;
		StringBuffer sb = new StringBuffer(8192);
		try {
			br = getBufferedReaderFromName(name, appletDocumentBase, null);
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			br.close();
		} catch (Exception e) {
			return null;
		}
		return sb.toString();
	}

  public static BufferedReader getBufferedReaderForInputStream(InputStream in)
      throws IOException {
    return new BufferedReader(new InputStreamReader(in));
  }
  
  public static BufferedReader getBufferedReaderForString(String data) {
    return (data == null ? null : new BufferedReader(new StringReader(data)));
  }

  public static BufferedReader getBufferedReaderFromName(String name, URL appletDocumentBase, String startCode)
      throws MalformedURLException, IOException {
    if (name == null)
      throw new IOException("Cannot find " + name);
    String path = classifyName(name, appletDocumentBase);
    return getUnzippedBufferedReaderFromName(path, appletDocumentBase, startCode);
  }

  /**
   * 
   * FileManager.classifyName
   * 
   * follow this with .replace('\\','/') and Escape.escape() to match Jmol's
   * file name in <PeakData file="...">
   * 
   * @param name
   * @param appletDocumentBase
   * @return name
   * @throws MalformedURLException
   */
  public static String classifyName(String name, URL appletDocumentBase)
      throws MalformedURLException {
    if (appletDocumentBase != null) {
      // This code is only for the applet
      if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
        name = "file:///" + name;
      //System.out.println("filemanager name " + name);
      //System.out.println("filemanager adb " + appletDocumentBase);
      URL url = new URL(appletDocumentBase, name);
      return url.toString();
    }

    // This code is for the app
    if (isURL(name)) {
      URL url = new URL(name);
      return url.toString();
    }
    File file = new File(name);
    return file.getAbsolutePath();
  }

  private final static String[] urlPrefixes = { "http:", "https:", "ftp:", 
  	SIMULATION_PROTOCOL, "file:" };

  public static boolean isURL(String name) {
  	for (int i = urlPrefixes.length; --i >= 0;)
  		if (name.startsWith(urlPrefixes[i]))
  			return true;
    return false;
  }
  
  private static BufferedReader getUnzippedBufferedReaderFromName(String name, URL appletDocumentBase, String startCode)
      throws IOException {
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) {
      subFileList = JSVTextFormat.split(name, "|");
      if (subFileList != null && subFileList.length > 0)
        name = subFileList[0];
    }
		if (name.startsWith(SIMULATION_PROTOCOL))
			return getBufferedReaderForString(getSimulationJCampDX(name.substring(SIMULATION_PROTOCOL.length())));
    InputStream in = getInputStream(name, true, appletDocumentBase);
    BufferedInputStream bis = new BufferedInputStream(in, 8192);
    if (isGzip(bis)) {
      return new BufferedReader(new InputStreamReader(new GZIPInputStream(bis)));
    } else if (JSVZipUtil.isZipFile(bis)) {
      return new JSVZipFileSequentialReader(bis, subFileList, startCode);
      //danger -- converting bytes to String here.
      //we lose 128-156 or so.
      //String s = (String) ZipUtil.getZipFileContents(bis, subFileList, 1);
      //bis.close();
      //return new BufferedReader(new StringReader(s));
    }
    return new BufferedReader(new InputStreamReader(bis));
  }

  private static boolean isGzip(InputStream is) throws IOException {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
  }

	public static InputStream getInputStream(String name, boolean showMsg,
			URL appletDocumentBase) throws IOException, MalformedURLException {
		boolean isURL = isURL(name);
		boolean isApplet = (appletDocumentBase != null);
		InputStream in;
		int length;
		String post = null;
		int iurl;
		if (isURL && (iurl = name.indexOf("?POST?")) >= 0) {
			post = name.substring(iurl + 6);
			name = name.substring(0, iurl);
		}
		if (isApplet || isURL) {
			URL url = (isApplet ? new URL(appletDocumentBase, name) : new URL(name));
			name = url.toString();
			if (showMsg)
				JSVLogger.info("JSVFileManager opening URL " + url.toString());
			URLConnection conn = url.openConnection();
			if (post != null) {
				conn.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				conn.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
				wr.write(post);
				wr.flush();
			}
			length = conn.getContentLength();
			in = conn.getInputStream();
		} else {
			if (showMsg)
				JSVLogger.info("JSVFileManager opening file " + name);
			File file = new File(name);
			length = (int) file.length();
			in = new FileInputStream(file);
		}
		return new JSVMonitorInputStream(in, length);
	}

	private static String nciResolver = "http://cactus.nci.nih.gov/chemical/structure/%FILE/file?format=sdf&get3d=True";
	private static String nmrdbServer = "http://www.nmrdb.org/tools/jmol/predict.php?POST?molfile=";

	private static Map<String, String> htSimulate;
	
	private static String getSimulationJCampDX(String name) {
		if (htSimulate == null)
			htSimulate = new Hashtable<String, String>();
		String key = "" + name.substring(name.indexOf("V2000") + 1).hashCode();
		String jcamp = htSimulate.get(key);
		if (jcamp == null) {
			System.out.println("creating " + name);
			boolean isInline = name.startsWith("MOL=");
			String molFile = (isInline ? JSVTextFormat.simpleReplace(name
					.substring(4), "\\n", "\n")
					: getFileAsString(JSVTextFormat.simpleReplace(nciResolver, "%FILE",
							JSVEscape.escapeUrl(name.substring(1))), null));
			int pt = molFile.indexOf("\n");
			molFile = "/JSpecView " + JSVersion.VERSION + molFile.substring(pt);
			molFile = JSVTextFormat.replaceAllCharacters(molFile, "?", '_');
			String json = getFileAsString(nmrdbServer + molFile, null);
			System.out.println(json);
			json = JSVTextFormat.simpleReplace(json, "\\r\\n", "\n");
			json = JSVTextFormat.simpleReplace(json, "\\t", "\t");
			json = JSVTextFormat.simpleReplace(json, "\\n", "\n");
			molFile = JSVParser.getQuotedJSONAttribute(json, "molfile", null);
			String xml = JSVParser.getQuotedJSONAttribute(json, "xml", null);
			xml = JSVTextFormat.simpleReplace(xml, "</", "\n</");
			xml = JSVTextFormat.simpleReplace(xml, "><", ">\n<");
			xml = JSVTextFormat.simpleReplace(xml, "\\\"", "\"");
			jcamp = JSVParser.getQuotedJSONAttribute(json, "jcamp", null);
			jcamp = "##TITLE=" + (isInline ? "JMOL SIMULATION" : name) + "\n"
					+ jcamp.substring(jcamp.indexOf("\n##") + 1);
			JSVLogger
					.info(jcamp.substring(0, jcamp.indexOf("##XYDATA") + 40) + "...");
			pt = 0;
			pt = jcamp.indexOf("##.");
			String id = name;
			int pt1 = id.indexOf("id='");
			if (isInline && pt1 > 0)
				id = id.substring(pt1 + 4, (id + "'").indexOf("'", pt1 + 4));
			jcamp = jcamp.substring(0, pt) + "##$MODELS=\n<Models>\n"
					+ "<ModelData id=" + JSVEscape.escape(id) + "\n type=\"MOL\">\n"
					+ molFile + "</ModelData>\n</Models>\n" + "##$SIGNALS=\n" + xml
					+ "\n" + jcamp.substring(pt);
			htSimulate.put(key, jcamp);
		}
		return jcamp;
	}

	private static URL getResource(Object object, String fileName, String[] error) {
    URL url = null;
    try {
      if ((url = object.getClass().getResource("resources/" + fileName)) == null)
        error[0] = "Couldn't find file: " + fileName;
    } catch (Exception e) {
    	
      error[0] = "Exception " + e.getMessage() + " in getResource "
          + fileName;
    }
    return url;
  }

  public static String getResourceString(Object object, String name, String[] error) {
    URL url = getResource(object, name, error);
    if (url == null) {
      error[0] = "Error loading resource " + name;
      return null;
    }
    StringBuffer sb = new StringBuffer();
    try {
      //  turns out from the Jar file
      //   it's a sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream
      //   and within Eclipse it's a BufferedInputStream
      //  LogPanel.log(name + " : " + url.getContent().toString());
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) url.getContent()));
      String line;
      while ((line = br.readLine()) != null)
        sb.append(line).append("\n");
      br.close();
    } catch (Exception e) {
      error[0] = e.getMessage();
    }
    String str = sb.toString();
    return str;
  }

  public static String getJmolFilePath(String filePath, URL appletDocumentBase) {
    try {
      filePath = classifyName(filePath, appletDocumentBase);
    } catch (MalformedURLException e) {
      return null;
    }
    return (appletDocumentBase == null ? filePath.replace('\\', '/') : filePath);
  }

  private static int stringCount;
  
  public static String getName(String file) {
  	if (file == null)
  		return "String" + (++stringCount);
    try {
      if (isURL(file)) {
      	if (file.startsWith(SIMULATION_PROTOCOL) && file.length() > 100)
      		return file.substring(0, Math.min(file.length(), 30)) + "...";
        String name = (new URL(file)).getFile();
        return name.substring(name.lastIndexOf('/') + 1);
      }
      return (new File(file)).getName();
    } catch (MalformedURLException e) {
      return null;
    }
  }

  public static void fileCopy(String name, File file) {
    try {
      BufferedReader br = JSVFileManager.getBufferedReaderFromName(name, null,
          null);
      FileWriter writer = new FileWriter(file.getAbsolutePath());
      String line = null;
      while ((line = br.readLine()) != null) {
        writer.write(line);
        writer.write(JSVTextFormat.newLine);
      }
      writer.close();
    } catch (Exception e) {
    	JSVLogger.error(e.getMessage());
    }
  }

}

class JSVMonitorInputStream extends FilterInputStream {
  int length;
  int position;
  int markPosition;
  int readEventCount;

  JSVMonitorInputStream(InputStream in, int length) {
    super(in);
    this.length = length;
    this.position = 0;
  }

  @Override
  public int read() throws IOException {
    ++readEventCount;
    int nextByte = super.read();
    if (nextByte >= 0)
      ++position;
    return nextByte;
  }

  @Override
  public int read(byte[] b) throws IOException {
    ++readEventCount;
    int cb = super.read(b);
    if (cb > 0)
      position += cb;
    return cb;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    ++readEventCount;
    int cb = super.read(b, off, len);
    if (cb > 0)
      position += cb;
    return cb;
  }

  @Override
  public long skip(long n) throws IOException {
    long cb = super.skip(n);
    // this will only work in relatively small files ... 2Gb
    position = (int) (position + cb);
    return cb;
  }

  @Override
  public void mark(int readlimit) {
    super.mark(readlimit);
    markPosition = position;
  }

  @Override
  public void reset() throws IOException {
    position = markPosition;
    super.reset();
  }

  int getPosition() {
    return position;
  }

  int getLength() {
    return length;
  }

  int getPercentageRead() {
    return position * 100 / length;
  }

}
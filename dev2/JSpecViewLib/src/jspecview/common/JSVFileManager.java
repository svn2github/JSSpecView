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

package jspecview.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.jmol.util.Logger;
import org.jmol.util.Parser;
import org.jmol.util.SB;
import org.jmol.util.Txt;

import jspecview.util.JSVEscape;
import jspecview.util.JSVTxt;
import jspecview.util.JSVZipFileSequentialReader;
import jspecview.util.JSVZipUtil;

public class JSVFileManager {

	// ALL STATIC METHODS
	
	public final static String SIMULATION_PROTOCOL = "http://SIMULATION/";

	public static URL appletDocumentBase;

	private static JSViewer viewer;
  
	public boolean isApplet() {
		return (appletDocumentBase != null);
	}

  public static String jsDocumentBase = "";
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
		SB sb = new SB();
		try {
			br = getBufferedReaderFromName(name, appletDocumentBase, null);
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.appendC('\n');
			}
			br.close();
		} catch (Exception e) {
			return null;
		}
		return sb.toString();
	}

  public static BufferedReader getBufferedReaderForInputStream(InputStream in) {
    try {
			return new BufferedReader(new InputStreamReader(in, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return null;
		}
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
      URL url = new URL(appletDocumentBase, name, null);
      return url.toString();
    }

    // This code is for the app
    if (isURL(name)) {
      URL url = new URL((URL) null, name, null);
      return url.toString();
    }
    File file = new File(name);
    return file.getAbsolutePath();
  }

  private final static String[] urlPrefixes = { "http:", "https:", "ftp:", 
  	SIMULATION_PROTOCOL, "file:" };

  public final static int URL_LOCAL = 4;


  public static boolean isURL(String name) {
  	for (int i = urlPrefixes.length; --i >= 0;)
  		if (name.startsWith(urlPrefixes[i]))
  			return true;
    return false;
  }
  
  public static int urlTypeIndex(String name) {
    for (int i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i])) {
        return i;
      }
    }
    return -1;
  }
  
  public static boolean isLocal(String fileName) {
    if (fileName == null)
      return false;
    int itype = urlTypeIndex(fileName);
    return (itype < 0 || itype == URL_LOCAL);
  }


  private static BufferedReader getUnzippedBufferedReaderFromName(String name, URL appletDocumentBase, String startCode)
      throws IOException {
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) {
      subFileList = Txt.split(name, "|");
      if (subFileList != null && subFileList.length > 0)
        name = subFileList[0];
    }
		if (name.startsWith(SIMULATION_PROTOCOL))
			return getBufferedReaderForString(getSimulationJCampDX(name.substring(SIMULATION_PROTOCOL.length())));
		
		if (viewer.isApplet) {
      Object ret = viewer.apiPlatform.getBufferedURLInputStream(new URL((URL) null, name, null), null, null);
      if (ret instanceof SB || ret instanceof String) {
      	return new BufferedReader(new StringReader(ret.toString()));
      } else if (JSVEscape.isAB(ret)) {
        return new BufferedReader(new StringReader(new String((byte[]) ret)));
      } else {
      	return new BufferedReader(new InputStreamReader((InputStream) ret, "UTF-8"));
      }

		}
    InputStream in = getInputStream(name, true, appletDocumentBase);
    BufferedInputStream bis = new BufferedInputStream(in);
    if (isGzip(bis)) {
      return new BufferedReader(new InputStreamReader(new GZIPInputStream(bis, 512), "UTF-8"));
    } else if (JSVZipUtil.isZipFile(bis)) {
      return new JSVZipFileSequentialReader(bis, subFileList, startCode);
      //danger -- converting bytes to String here.
      //we lose 128-156 or so.
      //String s = (String) ZipUtil.getZipFileContents(bis, subFileList, 1);
      //bis.close();
      //return new BufferedReader(new StringReader(s));
    }
    return new BufferedReader(new InputStreamReader(bis, "UTF-8"));
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
		//int length;
		String post = null;
		int iurl;
		if (isURL && (iurl = name.indexOf("?POST?")) >= 0) {
			post = name.substring(iurl + 6);
			name = name.substring(0, iurl);
		}
		if (isApplet || isURL) {
			URL url = new URL(appletDocumentBase, name, null);
			name = url.toString();
			if (showMsg)
				Logger.info("JSVFileManager opening URL " + url.toString());
			URLConnection conn = url.openConnection();
			if (post != null) {
				conn.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
				conn.setDoOutput(true);
				OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
				wr.write(post);
				wr.flush();
			}
			//length = conn.getContentLength();
			in = conn.getInputStream();
		} else {
			if (showMsg)
				Logger.info("JSVFileManager opening file " + name);
			File file = new File(name);
			//length = (int) file.length();
			in = new FileInputStream(file);
		}
		return in;//new InputStream(in, length);
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
			String molFile = (isInline ? Txt.simpleReplace(name
					.substring(4), "\\n", "\n")
					: getFileAsString(Txt.simpleReplace(nciResolver, "%FILE",
							JSVEscape.escapeUrl(name.substring(1))), null));
			int pt = molFile.indexOf("\n");
			molFile = "/JSpecView " + JSVersion.VERSION + molFile.substring(pt);
			molFile = Txt.simpleReplace(molFile, "?", "_");
			String json = getFileAsString(nmrdbServer + molFile, null);
			System.out.println(json);
			json = Txt.simpleReplace(json, "\\r\\n", "\n");
			json = Txt.simpleReplace(json, "\\t", "\t");
			json = Txt.simpleReplace(json, "\\n", "\n");
			molFile = JSVFileManager.getQuotedJSONAttribute(json, "molfile", null);
			String xml = JSVFileManager.getQuotedJSONAttribute(json, "xml", null);
			xml = Txt.simpleReplace(xml, "</", "\n</");
			xml = Txt.simpleReplace(xml, "><", ">\n<");
			xml = Txt.simpleReplace(xml, "\\\"", "\"");
			jcamp = JSVFileManager.getQuotedJSONAttribute(json, "jcamp", null);
			jcamp = "##TITLE=" + (isInline ? "JMOL SIMULATION" : name) + "\n"
					+ jcamp.substring(jcamp.indexOf("\n##") + 1);
			Logger
					.info(jcamp.substring(0, jcamp.indexOf("##XYDATA") + 40) + "...");
			pt = 0;
			pt = jcamp.indexOf("##.");
			String id = name;
			int pt1 = id.indexOf("id='");
			if (isInline && pt1 > 0)
				id = id.substring(pt1 + 4, (id + "'").indexOf("'", pt1 + 4));
			jcamp = jcamp.substring(0, pt) + "##$MODELS=\n<Models>\n"
					+ "<ModelData id=" + JSVEscape.eS(id) + "\n type=\"MOL\">\n"
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
    SB sb = new SB();
    try {
      //  turns out from the Jar file
      //   it's a sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream
      //   and within Eclipse it's a BufferedInputStream
      //  LogPanel.log(name + " : " + url.getContent().toString());
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) url.getContent(), "UTF-8"));
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
        String name = (new URL((URL) null, file, null)).getFile();
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
        writer.write(JSVTxt.newLine);
      }
      writer.close();
    } catch (Exception e) {
    	Logger.error(e.getMessage());
    }
  }

	public static String getQuotedJSONAttribute(String json, String key1,
			String key2) {
		if (key2 == null)
			key2 = key1;
		key1 = "\"" + key1 + "\":";
		key2 = "\"" + key2 + "\":";
		int pt1 = json.indexOf(key1);
		int pt2 = json.indexOf(key2, pt1);
		return (pt1 < 0 || pt2 < 0 ? null : Parser.getQuotedStringAt(json, pt2 + key2.length()));
	}

	public static void setDocumentBase(JSViewer v, URL documentBase) {
		viewer = v;
		appletDocumentBase = documentBase;
	}

}

// a nice idea, but never implemented; not relevant to JavaScript
//
//class JSVMonitorInputStream extends FilterInputStream {
//  int length;
//  int position;
//  int markPosition;
//  int readEventCount;
//
//  JSVMonitorInputStream(InputStream in, int length) {
//    super(in);
//    this.length = length;
//    this.position = 0;
//  }
//
//  /**
//   * purposely leaving off "Override" here for JavaScript
//   * 
//   * @j2sIgnore
//   */
//	public int read() throws IOException {
//    ++readEventCount;
//    int nextByte = super.read();
//    if (nextByte >= 0)
//      ++position;
//    return nextByte;
//  }
//  /**
//   * purposely leaving off "Override" here for JavaScript
//   * 
//   * @j2sIgnore
//   */
//  public int read(byte[] b) throws IOException {
//    ++readEventCount;
//    int cb = super.read(b);
//    if (cb > 0)
//      position += cb;
//    return cb;
//  }
//
//  @Override
//  public int read(byte[] b, int off, int len) throws IOException {
//    ++readEventCount;
//    int cb = super.read(b, off, len);
//    if (cb > 0)
//      position += cb;
//    return cb;
//  }
//
//  @Override
//  public long skip(long n) throws IOException {
//    long cb = super.skip(n);
//    // this will only work in relatively small files ... 2Gb
//    position = (int) (position + cb);
//    return cb;
//  }
//
//  @Override
//  public synchronized void mark(int readlimit) {
//    super.mark(readlimit);
//    markPosition = position;
//  }
//
//  @Override
//  public synchronized void reset() throws IOException {
//    position = markPosition;
//    super.reset();
//  }
//
//  int getPosition() {
//    return position;
//  }
//
//  int getLength() {
//    return length;
//  }
//
//  int getPercentageRead() {
//    return position * 100 / length;
//  }
//}
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javajs.util.AU;
import javajs.util.Encoding;
import javajs.util.OC;
import javajs.util.PT;
import javajs.util.SB;

import org.jmol.util.Logger;


import jspecview.api.JSVZipInterface;
import jspecview.util.JSVEscape;

public class JSVFileManager {

	// ALL STATIC METHODS
	
	public final static String SIMULATION_PROTOCOL = "http://SIMULATION/";
	  // possibly http://SIMULATION/MOL=...\n....\n....\n....

	public static URL appletDocumentBase;

	private static JSViewer viewer;
  
	public boolean isApplet() {
		return (appletDocumentBase != null);
	}

  public static String jsDocumentBase = "";
	/**
	 * @param name 
	 * @return file as string
	 * 
	 */

	public static String getFileAsString(String name) {
		if (name == null)
			return null;
		BufferedReader br;
		SB sb = new SB();
		try {
			br = getBufferedReaderFromName(name, null);
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

  public static BufferedReader getBufferedReaderFromName(String name, String startCode)
      throws MalformedURLException, IOException {
    if (name == null)
      throw new IOException("Cannot find " + name);
    Logger.info("JSVFileManager getBufferedReaderFromName " + name);
    String path = getFullPathName(name);
    Logger.info("JSVFileManager getBufferedReaderFromName " + path);
    return getUnzippedBufferedReaderFromName(path, startCode);
  }

	/**
	 * 
	 * FileManager.classifyName
	 * 
	 * follow this with .replace('\\','/') and Escape.escape() to match Jmol's
	 * file name in <PeakData file="...">
	 * 
	 * @param name
	 * @return name
	 * @throws MalformedURLException
	 */
	public static String getFullPathName(String name)
			throws MalformedURLException {
		if (appletDocumentBase == null) {
			// This code is for the app
			if (isURL(name)) {
				URL url = new URL((URL) null, name, null);
				return url.toString();
			}
			return viewer.apiPlatform.newFile(name).getFullPath();
		}
		// This code is only for the applet
		if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
			name = "file:///" + name;
		// System.out.println("filemanager name " + name);
		// System.out.println("filemanager adb " + appletDocumentBase);
		URL url = new URL(appletDocumentBase, name, null);
		return url.toString();
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


	private static BufferedReader getUnzippedBufferedReaderFromName(String name,
			String startCode) throws IOException {
		String[] subFileList = null;
		if (name.indexOf("|") >= 0) {
			subFileList = PT.split(name, "|");
			if (subFileList != null && subFileList.length > 0)
				name = subFileList[0];
		}
		if (name.startsWith(SIMULATION_PROTOCOL))
			return getBufferedReaderForString(getNMRSimulationJCampDX(name
					.substring(SIMULATION_PROTOCOL.length())));

		if (viewer.isApplet) {
			Object ret = viewer.apiPlatform.getBufferedURLInputStream(new URL(
					(URL) null, name, null), null, null);
			if (ret instanceof SB || ret instanceof String) {
				return new BufferedReader(new StringReader(ret.toString()));
			} else if (isAB(ret)) {
				return new BufferedReader(new StringReader(new String((byte[]) ret)));
			} else {
				return new BufferedReader(new InputStreamReader((InputStream) ret,
						"UTF-8"));
			}

		}
		InputStream in = getInputStream(name, true, null);
		BufferedInputStream bis = new BufferedInputStream(in);
		in = bis;
		if (isZipFile(bis))
			return ((JSVZipInterface) JSViewer
					.getInterface("jspecview.util.JSVZipUtil"))
					.newJSVZipFileSequentialReader(in, subFileList, startCode);
		if (isGzip(bis))
			in = ((JSVZipInterface) JSViewer
					.getInterface("jspecview.util.JSVZipUtil")).newGZIPInputStream(in);
		return new BufferedReader(new InputStreamReader(in, "UTF-8"));
	}

  public static boolean isAB(Object x) {
    /**
     * @j2sNative
     *  return Clazz.isAI(x);
     */
    {
    return x instanceof byte[];
    }
  }
  
  public static boolean isZipFile(InputStream is) throws IOException {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x50 && abMagic[1] == (byte) 0x4B
        && abMagic[2] == (byte) 0x03 && abMagic[3] == (byte) 0x04);
  }

  private static boolean isGzip(InputStream is) throws IOException {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
  }
  
	public static Object getStreamAsBytes(BufferedInputStream bis,
			OC out) throws IOException {
		byte[] buf = new byte[1024];
		byte[] bytes = (out == null ? new byte[4096] : null);
		int len = 0;
		int totalLen = 0;
		while ((len = bis.read(buf, 0, 1024)) > 0) {
			totalLen += len;
			if (out == null) {
				if (totalLen >= bytes.length)
					bytes = AU.ensureLengthByte(bytes, totalLen * 2);
				System.arraycopy(buf, 0, bytes, totalLen - len, len);
			} else {
				out.write(buf, 0, len);
			}
		}
		bis.close();
		if (out == null) {
			return AU.arrayCopyByte(bytes, totalLen);
		}
		return totalLen + " bytes";
	}

  public static String postByteArray(String fileName, byte[] bytes) {
    Object ret = null;    
    try {
			ret = getInputStream(fileName, false, bytes);
		} catch (Exception e) {
			return e.toString();
		}
    if (ret instanceof String)
      return (String) ret;
    try {
      ret = getStreamAsBytes((BufferedInputStream) ret, null);
    } catch (IOException e) {
      try {
        ((BufferedInputStream) ret).close();
      } catch (IOException e1) {
        // ignore
      }
    }
    return (ret == null ? "" : fixUTF((byte[]) ret));
  }

  private static Encoding getUTFEncoding(byte[] bytes) {
    if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF)
      return Encoding.UTF8;
    if (bytes.length >= 4 && bytes[0] == (byte) 0 && bytes[1] == (byte) 0 
        && bytes[2] == (byte) 0xFE && bytes[3] == (byte) 0xFF)
      return Encoding.UTF_32BE;
    if (bytes.length >= 4 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE 
        && bytes[2] == (byte) 0 && bytes[3] == (byte) 0)
      return Encoding.UTF_32LE;
    if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)
      return Encoding.UTF_16LE;
    if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)
      return Encoding.UTF_16BE;
    return Encoding.NONE;

  }

  public static String fixUTF(byte[] bytes) {
    
    Encoding encoding = getUTFEncoding(bytes);
    if (encoding != Encoding.NONE)
    try {
      String s = new String(bytes, encoding.name().replace('_', '-'));
      switch (encoding) {
      case UTF8:
      case UTF_16BE:
      case UTF_16LE:
        // extra byte at beginning removed
        s = s.substring(1);
        break;
      default:
        break;        
      }
      return s;
    } catch (UnsupportedEncodingException e) {
      System.out.println(e);
    }
    return new String(bytes);
  }


	public static InputStream getInputStream(String name, boolean showMsg, byte[] postBytes) throws IOException, MalformedURLException {
		boolean isURL = isURL(name);
		boolean isApplet = (appletDocumentBase != null);
		Object in = null;
		//int length;
		String post = null;
		int iurl;
		if (isURL && (iurl = name.indexOf("?POST?")) >= 0) {
			post = name.substring(iurl + 6);
			name = name.substring(0, iurl);
		}
		if (isApplet || isURL) {
			URL url = new URL(appletDocumentBase, name, null);
			Logger.info("JSVFileManager opening URL " + url + (post == null ? "" : " with POST of " + post.length() + " bytes"));
			in = viewer.apiPlatform.getBufferedURLInputStream(url, postBytes, post);
			if (in instanceof String) {
				Logger.info("JSVFileManager could not get this URL:" + in);
				return null;
			}
		} else {
			if (showMsg)
				Logger.info("JSVFileManager opening file " + name);
			in = viewer.apiPlatform.getBufferedFileInputStream(name);
		}
		return (InputStream) in;
	}

	private static String nciResolver = "http://cactus.nci.nih.gov/chemical/structure/%FILE/file?format=sdf";
	private static String nmrdbServer = "http://www.nmrdb.org/tools/jmol/predict.php?POST?molfile=";

	private static Map<String, String> htSimulate;

	/**
	 * Accepts either $chemicalname or MOL=molfiledata Queries NMRDB or NIH+NMRDB
	 * to get predicted spetrum
	 * 
	 * TODO: how about adding spectrometer frequency?
	 * TODO: options for other data types? 2D? IR?
	 * 
	 * @param name
	 * @return jcamp data
	 */
	private static String getNMRSimulationJCampDX(String name) {
		if (htSimulate == null)
			htSimulate = new Hashtable<String, String>();
		String key = "" + name.substring(name.indexOf("V2000") + 1).hashCode();
		String jcamp = htSimulate.get(key);
		if (jcamp != null)
			return jcamp;
		boolean isInline = name.startsWith("MOL=");
		String molFile;
		if ((molFile = (isInline ? PT.simpleReplace(name.substring(4), "\\n", "\n")
				: getFileAsString(PT.simpleReplace(nciResolver, "%FILE",
						PT.escapeUrl(name.substring(1)))))) == null)
			Logger.info("no data returned");
		// null here throws an exception
		int pt = molFile.indexOf("\n");
		molFile = "/JSpecView " + JSVersion.VERSION + molFile.substring(pt);
		molFile = PT.simpleReplace(molFile, "?", "_");
		String json = getFileAsString(nmrdbServer + molFile);
		System.out.println(json);
		json = PT.simpleReplace(json, "\\r\\n", "\n");
		json = PT.simpleReplace(json, "\\t", "\t");
		json = PT.simpleReplace(json, "\\n", "\n");
		molFile = JSVFileManager.getQuotedJSONAttribute(json, "molfile", null);
		String xml = JSVFileManager.getQuotedJSONAttribute(json, "xml", null);
		xml = PT.simpleReplace(xml, "</", "\n</");
		xml = PT.simpleReplace(xml, "><", ">\n<");
		xml = PT.simpleReplace(xml, "\\\"", "\"");
		jcamp = JSVFileManager.getQuotedJSONAttribute(json, "jcamp", null);
		jcamp = "##TITLE=" + (isInline ? "JMOL SIMULATION" : name) + "\n"
				+ jcamp.substring(jcamp.indexOf("\n##") + 1);
		Logger.info(jcamp.substring(0, jcamp.indexOf("##XYDATA") + 40) + "...");
		pt = 0;
		pt = jcamp.indexOf("##.");
		String id = name;
		int pt1 = id.indexOf("id='");
		if (isInline && pt1 > 0)
			id = id.substring(pt1 + 4, (id + "'").indexOf("'", pt1 + 4));
		jcamp = jcamp.substring(0, pt) + "##$MODELS=\n<Models>\n"
				+ "<ModelData id=" + JSVEscape.eS(id) + "\n type=\"MOL\">\n" + molFile
				+ "</ModelData>\n</Models>\n" + "##$SIGNALS=\n" + xml + "\n"
				+ jcamp.substring(pt);
		htSimulate.put(key, jcamp);
		return jcamp;
	}

	private static URL getResource(Object object, String fileName, String[] error) {
    URL url = null;
    try {
      if ((url = object.getClass().getResource(fileName)) == null)
        error[0] = "Couldn't find file: " + fileName;
    } catch (Exception e) {
    	
      error[0] = "Exception " + e.getMessage() + " in getResource "
          + fileName;
    }
    return url;
  }

  public static String getResourceString(Object object, String name, String[] error) {
    Object url = getResource(object, name, error);
    if (url == null) {
      error[0] = "Error loading resource " + name;
      return null;
    }
    if (url instanceof String) {
    	// JavaScript does this -- all resources are just files on the site somewhere
    	return getFileAsString((String) url);
    }
    SB sb = new SB();
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(
          (InputStream) ((URL) url).getContent(), "UTF-8"));
      String line;
      while ((line = br.readLine()) != null)
        sb.append(line).append("\n");
      br.close();
    } catch (Exception e) {
      error[0] = e.getMessage();
    }
    return sb.toString();
  }

  public static String getJmolFilePath(String filePath) {
    try {
      filePath = getFullPathName(filePath);
    } catch (MalformedURLException e) {
      return null;
    }
    return (appletDocumentBase == null ? filePath.replace('\\', '/') : filePath);
  }

  private static int stringCount;

	public static String getName(String fileName) {
		if (fileName == null)
			return "String" + (++stringCount);
		if (isURL(fileName)) {
			try {
				if (fileName.startsWith(SIMULATION_PROTOCOL) && fileName.length() > 100)
					return fileName.substring(0, Math.min(fileName.length(), 30)) + "...";
				String name = (new URL((URL) null, fileName, null)).getFile();
				return name.substring(name.lastIndexOf('/') + 1);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return viewer.apiPlatform.newFile(fileName).getName();
	}

	public static String getQuotedJSONAttribute(String json, String key1,
			String key2) {
		if (key2 == null)
			key2 = key1;
		key1 = "\"" + key1 + "\":";
		key2 = "\"" + key2 + "\":";
		int pt1 = json.indexOf(key1);
		int pt2 = json.indexOf(key2, pt1);
		return (pt1 < 0 || pt2 < 0 ? null : PT.getQuotedStringAt(json, pt2 + key2.length()));
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

/* Copyright (c) 2002-2007 The University of the West Indies
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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

public class FileManager {

  
  private URL appletDocumentBase = null;
  private String openErrorMessage;

  /**
   * From org.jmol.viewer.FileManager
   * 
   * @param appletDocumentBase 
   * 
   */
  
  public FileManager (URL appletDocumentBase) {
    this.appletDocumentBase = appletDocumentBase;
  }
  
  public String getFileAsString(String name) {
    String[] data = new String[2];
    data[0] = name;
    // ignore error completely
    getFileDataOrErrorAsString(data);
    return data[1];
  }
  
  public boolean getFileDataOrErrorAsString(String[] data) {
    data[1] = "";
    String name = data[0];
    if (name == null)
      return false;
    Object t = getBufferedReaderOrErrorMessageFromName(name, data);
    if (t instanceof String) {
      data[1] = (String) t;
      return false;
    }
    try {
      BufferedReader br = (BufferedReader) t;
      StringBuffer sb = new StringBuffer(8192);
      String line;
      int nBytesRead = 0;
      while ((line = br.readLine()) != null) {
        nBytesRead += line.length();
        sb.append(line);
        sb.append('\n');
      }
      br.close();
      data[1] = sb.toString();
      return true;
    } catch (Exception ioe) {
      data[1] = ioe.getMessage();
      return false;
    }
  }

  Object getBufferedReaderOrErrorMessageFromName(String name,
                                                 String[] fullPathNameReturn) {
    String[] names = classifyName(name);
    if (openErrorMessage != null)
      return openErrorMessage;
    if (names == null)
      return "cannot read file name: " + name;
    if (fullPathNameReturn != null)
      fullPathNameReturn[0] = names[0].replace('\\', '/');
    return getUnzippedBufferedReaderOrErrorMessageFromName(names[0], false, false);
  }

  private String[] classifyName(String name) {
    if (name == null)
      return null;
    String[] names = new String[2];
    if (appletDocumentBase != null) {
      // This code is only for the applet
      try {
        if (name.indexOf(":\\") == 1 || name.indexOf(":/") == 1)
          name = "file:///" + name;
        //System.out.println("filemanager name " + name);
        //System.out.println("filemanager adb " + appletDocumentBase);
        URL url = new URL(appletDocumentBase, name);
        names[0] = url.toString();
        // we add one to lastIndexOf(), so don't worry about -1 return value
        names[1] = names[0].substring(names[0].lastIndexOf('/') + 1,
                names[0].length());
        //System.out.println("filemanager 0 " + names[0]);
        //System.out.println("filemanager 1 " + names[1]);
      } catch (MalformedURLException e) {
        openErrorMessage = e.getMessage();
      }
      return names;
    }
    // This code is for the app
    int i = urlTypeIndex(name);
    if (i >= 0) {
      try {
        URL url = new URL(name);
        names[0] = url.toString();
        names[1] = names[0].substring(names[0].lastIndexOf('/') + 1,
            names[0].length());
      } catch (MalformedURLException e) {
        openErrorMessage = e.getMessage();
      }
      return names;
    }
    File file = new File(name);
    names[0] = file.getAbsolutePath();
    names[1] = file.getName();
    return names;
  }

  private final static String[] urlPrefixes = {"http:", "https:", "ftp:", "file:"};

  private static int urlTypeIndex(String name) {
    for (int i = 0; i < urlPrefixes.length; ++i) {
      if (name.startsWith(urlPrefixes[i])) {
        return i;
      }
    }
    return -1;
  }
  
  Object getUnzippedBufferedReaderOrErrorMessageFromName(String name,
                                                         boolean allowZipStream,
                                                         boolean isTypeCheckOnly) {
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) 
      name = (subFileList = TextFormat.split(name, "|"))[0];
    Object t = getInputStreamOrErrorMessageFromName(name, true);
    if (t instanceof String)
      return t;
    try {
      BufferedInputStream bis = new BufferedInputStream((InputStream)t, 8192);
      InputStream is = bis;
      if (isGzip(is)) {
        is = new GZIPInputStream(bis);
      } else if (ZipUtil.isZipFile(is)) {
        if (allowZipStream)
          return new ZipInputStream(bis);
        //danger -- converting bytes to String here. 
        //we lose 128-156 or so.
        String s = (String) ZipUtil.getZipFileContents(is, subFileList, 1);
        is.close();
        return getBufferedReaderForString(s);
      }
      return new BufferedReader(new InputStreamReader(is));
    } catch (Exception ioe) {
      return ioe.getMessage();
    }
  }

  BufferedReader getBufferedReaderForString(String string) {
    return new BufferedReader(new StringReader(string));
  }

  Object getInputStreamOrErrorMessageFromName(String name, boolean showMsg) {
    return getInputStream(name, showMsg, appletDocumentBase);    
  }
  
  static boolean isGzip(InputStream is) throws Exception {
    byte[] abMagic = new byte[4];
    is.mark(5);
    int countRead = is.read(abMagic, 0, 4);
    is.reset();
    return (countRead == 4 && abMagic[0] == (byte) 0x1F && abMagic[1] == (byte) 0x8B);
  }

  public static Object getInputStream(String name, boolean showMsg, URL appletDocumentBase) {
    //System.out.println("inputstream for " + name);
    String errorMessage = null;
    int iurlPrefix;
    for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0;)
      if (name.startsWith(urlPrefixes[iurlPrefix]))
        break;
    boolean isURL = (iurlPrefix >= 0);
    boolean isApplet = (appletDocumentBase != null);
    InputStream in;
    int length;
    try {
      if (isApplet || isURL) {
        URL url = (isApplet ? new URL(appletDocumentBase, name) : new URL(name));
        name = url.toString();
        if (showMsg)
          Logger.info("FileManager opening URL " + url.toString());
        URLConnection conn = url.openConnection();
        length = conn.getContentLength();
        in = conn.getInputStream();
      } else {
        if (showMsg)
          Logger.info("FileManager opening file " + name);
        File file = new File(name);
        System.out.println(file);
        length = (int) file.length();
        in = new FileInputStream(file);
        System.out.println(in);
      }
      return new MonitorInputStream(in, length);
    } catch (Exception e) {
      errorMessage = "" + e;
    }
    return errorMessage;
  }

  public URL getResource(Object object, String fileName, boolean flagError) {
    URL url = null;
    try {
      if ((url = object.getClass().getResource("resources/" + fileName)) == null
          && flagError)
        openErrorMessage = "Couldn't find file: " + fileName;
    } catch (Exception e) {
      openErrorMessage = "Exception " + e.getMessage() + " in getResource "
          + fileName;
    }
    return url;
  }
  
  public String getResourceString(Object object, String name, boolean flagError) {
    URL url = getResource(object, name, flagError);
    if (url == null) {
      openErrorMessage = "Error loading resource " + name;
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
      openErrorMessage = e.getMessage();
    }
    String str = sb.toString();
    return str;
  }

  public String getErrorMessage() {
    return openErrorMessage;
  }
}

class MonitorInputStream extends FilterInputStream {
  int length;
  int position;
  int markPosition;
  int readEventCount;

  MonitorInputStream(InputStream in, int length) {
    super(in);
    this.length = length;
    this.position = 0;
  }

  @Override
  public int read() throws IOException{
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
    position = (int)(position + cb);
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

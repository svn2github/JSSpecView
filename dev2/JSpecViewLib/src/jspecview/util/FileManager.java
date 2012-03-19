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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

public class FileManager {

  private URL appletDocumentBase;
  private String openErrorMessage;

  /**
   * From org.jmol.viewer.FileManager
   * 
   * @param appletDocumentBase
   * 
   */

  public FileManager(URL appletDocumentBase) {
    this.appletDocumentBase = appletDocumentBase;
  }

  public String getFileAsString(String name) throws IOException {
    if (name == null)
      throw new IOException("name is null");

    BufferedReader br = getBufferedReaderFromName(name, appletDocumentBase, null);

    StringBuffer sb = new StringBuffer(8192);
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
      sb.append('\n');
    }
    br.close();
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
   * @return
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
      "file:" };

  public static boolean isURL(String name) {
    for (String prefix : urlPrefixes) {
      if (name.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private static BufferedReader getUnzippedBufferedReaderFromName(String name, URL appletDocumentBase, String startCode)
      throws IOException {
    String[] subFileList = null;
    if (name.indexOf("|") >= 0) {
      subFileList = TextFormat.split(name, "|");
      if (subFileList != null && subFileList.length > 0)
        name = subFileList[0];
    }
    InputStream in = getInputStream(name, true, appletDocumentBase);
    BufferedInputStream bis = new BufferedInputStream(in, 8192);
    if (isGzip(bis)) {
      return new BufferedReader(new InputStreamReader(new GZIPInputStream(bis)));
    } else if (ZipUtil.isZipFile(bis)) {
      return new ZipFileSequentialReader(bis, subFileList, startCode);
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
                                           URL appletDocumentBase)
      throws IOException, MalformedURLException {
    int iurlPrefix;
    for (iurlPrefix = urlPrefixes.length; --iurlPrefix >= 0;)
      if (name.startsWith(urlPrefixes[iurlPrefix]))
        break;
    boolean isURL = (iurlPrefix >= 0);
    System.out.println("FM getInputStream "  + isURL + " " + name);
    boolean isApplet = (appletDocumentBase != null);
    InputStream in;
    int length;
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
      length = (int) file.length();
      in = new FileInputStream(file);
    }
    return new MonitorInputStream(in, length);
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

  public static String getJmolFilePath(String filePath, URL appletDocumentBase) {
    try {
      filePath = classifyName(filePath, appletDocumentBase);
    } catch (MalformedURLException e) {
      return null;
    }
    return (appletDocumentBase == null ? filePath.replace('\\', '/') : filePath);
  }

  public static String getName(String file) {
    try {
      if (isURL(file)) {
        String name = (new URL(file)).getFile();
        return name.substring(name.lastIndexOf('/') + 1);
      }
      return (new File(file)).getName();
    } catch (MalformedURLException e) {
      return null;
    }
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

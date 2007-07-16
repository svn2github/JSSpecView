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

package jspecview.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;

import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;
import jspecview.util.JDXSourceStringTokenizer;
import jspecview.util.JSpecViewUtils;

/**
 * Class <code>JDXSourceFactory</code> creates an instance of a JDXSource from
 * an <code>InputStream</code>. It determines the type of source from the stream
 * and creates the instance of the corresponding class. Either <code>SimpleSource,
 * BlockSource or NTupleSource</code>
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 * @see jspecview.common.JDXSource
 */

public class JDXSourceFactory {

  /**
   * Blocks Label
   */
  private final String BLOCKS_LABEL = "LINK";

  /**
   * Ntuples label
   */
  private final String NTUPLES_LABEL = "NTUPLES";

  /** Indicates a Simple Source */
  public final int SIMPLE = 0;
  /** Indicates a Block Source */
  public final int BLOCK = 1;
  /** Indicates a Ntuple Source */
  public final int NTUPLE = 2;

  // The input Stream
  protected InputStream inputStream;

  /**
   * Constructor
   * @param in the source input stream
   */
  public JDXSourceFactory(InputStream in){
    inputStream = in;
  }

  /**
   * Creates an instance of a JDXSource
   * @return the JDXSource
   * @throws JSpecViewException
   */
  public JDXSource createJDXSource() throws JSpecViewException{
    String sourceContents;

    // for debugging;
    long time1, time2, totalTime;
    time1 = 0;
    // for debugging

    if(JSpecViewUtils.DEBUG){
      System.out.print("Started Reading Source at: ");
      System.out.println(Calendar.getInstance().getTime());
      time1 = Calendar.getInstance().getTimeInMillis();
    }

    sourceContents = getSourceContents();

    if(JSpecViewUtils.DEBUG){
      System.out.print("Finished Reading Source at: ");
      System.out.println(Calendar.getInstance().getTime());
      time2 = Calendar.getInstance().getTimeInMillis();
      totalTime = time2 - time1;
      System.out.println("Total time = " +  totalTime + "ms or " + ((double)totalTime/1000) + "s");
    }

    if(sourceContents == null)
      throw new JDXSourceException("Unable to Read Source");

    int sourceType = determineJDXSourceType(sourceContents);
    if(sourceType == -1){
      throw new JDXSourceException("JDX Source Type not Recognized");
    }

    switch(sourceType){
      case SIMPLE: return SimpleSource.getInstance(sourceContents);
      case BLOCK: return BlockSource.getInstance(sourceContents);
      case NTUPLE: return NTupleSource.getInstance(sourceContents);
        //return RestrictedNTupleSource.getInstance(sourceContents, 128);
    }

    return null;
  }

  /**
   * Returns the contents of the file as a string
   * @return the contents of the file as a string
   */
  private String getSourceContents(){
    StringBuffer contents = new StringBuffer();

    try
    {
      BufferedReader d = new BufferedReader(
        new InputStreamReader(inputStream)
      );

      String line = null;

      while ((line = d.readLine()) != null)
      {/*
        line = line.trim();
        int commentIndex = line.indexOf("$$");

        // ignore comments that start at the beginning of the line
        // or empty lines
        if(line.length() == 0 || commentIndex == 0)
          continue;

        // remove comments from the end of a line
        if(commentIndex != -1)
          line = line.substring(0, commentIndex).trim();
        */
        if(line.length() != 0)
          contents.append(line + "\n");
      }
    }
    catch (IOException ioe){
      return null;
    }

    return contents.toString();
  }


  /**
   * Determines the type of JDX Source
   * @param sourceContents the contents of the source
   * @return the JDX source type
   */
  private int determineJDXSourceType(String sourceContents){
    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);
    String label;
    while(t.hasMoreTokens()){
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);
      if (label.equals("##DATATYPE") && t.value.toUpperCase().equals(BLOCKS_LABEL))
        return BLOCK;
      if (label.equals("##DATACLASS") && t.value.toUpperCase().equals(NTUPLES_LABEL))
          return NTUPLE;
      Arrays.sort(JDXSource.TABULAR_DATA_LABELS);
      if(Arrays.binarySearch(JDXSource.TABULAR_DATA_LABELS, label) > 0)
        return SIMPLE;
      }
    return -1;
  }

  /**
   * Returns the input Stream for this JDXSourceFactory
   * @return the Source inputStream
   */
  public InputStream getInputStream(){
    return inputStream;
  }

}

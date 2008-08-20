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
import java.io.StringReader;
import java.util.ArrayList;
//import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;
import jspecview.exception.SourceTypeUnsupportedException;

/**
 * Representation of an JCAMP-DX NTuple Source.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */
public class NTupleSource extends CompoundSource {

  /**
   * Constructs a new NTupleSource from the source Listing
   * @throws JSpecViewException
   */
  protected NTupleSource() throws JSpecViewException{
    super();
  }

  /**
   * Does the actual work of initializing the Source instance
   * @param sourceContents the contents of the source as a String
   * @throws JSpecViewException
   * @return an instance of an NTupleSource
   */
  public static NTupleSource getInstance(String sourceContents) throws JSpecViewException{

    String tabularSpecData = null;
    JDXSpectrum spectrum;
    HashMap<String,String> LDRTable;
    HashMap<String,String> sourceLDRTable = new HashMap<String,String>();
    HashMap<String,ArrayList<String>> nTupleTable = new HashMap<String,ArrayList<String>>();

    double offset = Graph.ERROR;
    double obFreq = Graph.ERROR;
    double firstX, lastX, xFactor = 1;
    double deltaX = 0, yFactor = 1;
    boolean increasing = true, continuous = true;
    int nPoints;
    String xUnits= "", title = "", yUnits = "";
    String page = "";

    int dataPointNum = -1;
    int shiftRefType = -1;

    // True if we have a long date
    //boolean longDateFound = false;
    //String date = "";
    //String time = "";

    String[] plotSymbols = new String[2];

    int tabDataLineNo = 0;

    Coordinate[] xyCoords;

    NTupleSource ns = new NTupleSource();

    StringBuffer errorLog = new StringBuffer();
    String errorSeparator="________________________________________________________";
    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);

    // Read Source Specific Header
    String label = "";
    while(t.hasMoreTokens() && !label.equals("##NTUPLES")){
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);

      if(label.equals("##TITLE")){
        if(t.value != null && !t.value.equals(""))
          title = t.value;
        else
          title = "Unknown";
        ns.setTitle(title);
        continue;
      }

      if(label.equals("##JCAMPDX")){
        ns.setJcampdx(t.value);
        double version = Double.parseDouble(t.value);
        if(version >= 6.0){
          errorLog.append("JCAMP-DX 6 Source Type is Unsupported\n");
          throw new SourceTypeUnsupportedException("JCAMP-DX Source Type is Unsupported");
        }
        continue;
      }

      if(label.equals("##ORIGIN")){
        if(t.value != null && !t.value.equals(""))
          ns.setOrigin(t.value);
        else
          ns.setOrigin("Unknown");
        continue;
      }

      if(label.equals("##OWNER")){
        if(t.value != null && !t.value.equals(""))
          ns.setOwner(t.value);
        else
          ns.setOwner("Unknown");
        continue;
      }

      if(label.equals("##DATATYPE")){
        ns.setDataType(t.value);
        continue;
      }

      if (label.equals("##LONGDATE")) {
          ns.setLongDate(t.value);
          //longDateFound = true;
     }

     if (label.equals("##DATE"))  {
//            notesLDRTable.put(label, t.value);
           ns.setDate(t.value);
     }

     if (label.equals("##TIME")) {
//            notesLDRTable.put(label, t.value);
           ns.setTime(t.value);
     }
      if(label.equals("##PATHLENGTH")){
        ns.setPathlength(t.value);
      }

      if( (label.equals("##$OFFSET")) && (shiftRefType != 0) )  {
        offset = Double.parseDouble(t.value);
        // bruker doesn't need dataPointNum
        dataPointNum = 1;
        // bruker type
        shiftRefType = 1;
        continue;
      }

      if( (label.equals("##$REFERENCEPOINT")) && (shiftRefType != 0) )  {
        offset = Double.parseDouble(t.value);
        // varian doesn't need dataPointNum
        dataPointNum = 1;
        // varian type
        shiftRefType = 2;
        continue;
      }

      if(label.equals("##.SHIFTREFERENCE")){
        if ( !(((ns.getDataType()).toUpperCase()).contains("SPECTRUM")) )
          continue;
        StringTokenizer srt = new StringTokenizer(t.value, ",");
        if(srt.countTokens() !=4 )
          continue;
        try{
          srt.nextToken();
          srt.nextToken();
          dataPointNum = Integer.parseInt(srt.nextToken().trim());
          offset = Double.parseDouble(srt.nextToken().trim());
        }
        catch(NumberFormatException nfe){
          continue;
        }
        catch(NoSuchElementException nsee){
          continue;
        }

        if (dataPointNum <= 0)
          dataPointNum = 1;

        shiftRefType = 0;
        continue;
      }

      if(label.equals("##.OBSERVEFREQUENCY")){
        obFreq = Double.parseDouble(t.value);
        sourceLDRTable.put(label, t.value);
 //       continue;
      }

      sourceLDRTable.put(label, t.value);
    } //Finished Pulling out the LDR Table Data

    ns.setHeaderTable(sourceLDRTable);

    /*--------------------------------------------*/
    /*------------- Fetch Page Data --------------*/

    if(!label.equals("##NTUPLES"))
      throw new JSpecViewException("Invalid NTuple Source");

    // Read NTuple Table
    StringTokenizer st;
    ArrayList<String> attrList;
    while(t.hasMoreTokens()){
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);
      if(label.equals("##PAGE")){
        break;
      }
      st = new StringTokenizer(t.value, ",");
      attrList = new ArrayList<String>();
      while(st.hasMoreTokens()){
        attrList.add(st.nextToken().trim());
      }
      nTupleTable.put(label, attrList);
    }//Finised With Page Data
    /*--------------------------------------------*/

    if(!label.equals("##PAGE"))
      throw new JSpecViewException("Error Reading NTuple Source");


    /*--------------------------------------------*/
    /*-------- Gather Spectra Data From File -----*/

    // Create and add Spectra
    spectrum = new JDXSpectrum();
    while(t.hasMoreTokens()){

      if(label.equals("##ENDNTUPLES")){
        break;
      }

      if(label.equals("##PAGE")){
        page = t.value;
        t.nextToken(); // ignore ##PAGE
        label = JSpecViewUtils.cleanLabel(t.label);
        continue;
      }

      LDRTable = new HashMap<String, String>();
      while(!label.equals("##DATATABLE")){
        LDRTable.put(t.label, t.value);
        t.nextToken();
        label = JSpecViewUtils.cleanLabel(t.label);
      }

      if(label.equals("##DATATABLE")){
        tabDataLineNo = t.labelLineNo;
        // determine if continuous
        String dtblStr = t.value;
        try{
          BufferedReader reader = new BufferedReader(new StringReader(dtblStr));
          String line = reader.readLine();
          if(line.trim().indexOf("PEAKS") > 0){
            continuous =  false;
          }

          // parse variable list
          int index1 = line.indexOf('(');
          int index2 = line.lastIndexOf(')');
          if(index1 == -1 || index2 == -1)
            throw new JDXSourceException("Variable List not Found");
          String varList = line.substring(index1, index2+1);

          ArrayList<String> symbols = (ArrayList<String>) nTupleTable.get("##SYMBOL");
          int countSyms = 0;
          for(int i = 0; i < symbols.size(); i++){
            String sym = ((String)symbols.get(i)).trim();
            if(varList.indexOf(sym) != -1){
              plotSymbols[countSyms++] = sym;
            }
            if(countSyms == 2)
              break;
          }

        }
        catch(IOException ioe){
        }


        if(continuous){
          spectrum.setContinuous(true);
          spectrum.setDataClass("XYDATA");
        }
        else{
          spectrum.setContinuous(false);
          spectrum.setDataClass("PEAKTABLE");
        }
      }

      // Get Tabular Spectral Data
      tabularSpecData = t.value;
      if(tabularSpecData == null)
        throw new JSpecViewException("Error Reading Data Set");

      String tmp = tabularSpecData;

      try{
        char chr;
        do{
          tmp = tmp.substring(tmp.indexOf("\n") + 1);
          chr = tmp.trim().charAt(0);
        }
        while(!Character.isDigit(chr) && chr != '+' && chr != '-' && chr != '.');
      }
      catch(IndexOutOfBoundsException iobe){
        throw new JSpecViewException("Error Reading Data Set");
      }
      tabularSpecData = tmp;

      ArrayList<String> list;
      if(spectrum.getDataClass().equals("XYDATA")){
        // Get Label Values

        list = (ArrayList<String>)nTupleTable.get("##SYMBOL");
        int index1 = list.indexOf(plotSymbols[0]);
        int index2 = list.indexOf(plotSymbols[1]);

        list = (ArrayList<String>)nTupleTable.get("##FACTOR");
        xFactor = Double.parseDouble((String)list.get(index1));
        yFactor = Double.parseDouble((String)list.get(index2));

        list = (ArrayList<String>)nTupleTable.get("##LAST");
        lastX = Double.parseDouble((String)list.get(index1));
        //lastY = Double.parseDouble((String)list.get(index1));

        list = (ArrayList<String>)nTupleTable.get("##FIRST");
        firstX = Double.parseDouble((String)list.get(index1));
        //firstY = Double.parseDouble((String)list.get(index2));

        list = (ArrayList<String>)nTupleTable.get("##VARDIM");
        nPoints = Integer.parseInt((String)list.get(index1));

        list = (ArrayList<String>)nTupleTable.get("##UNITS");
        xUnits = (String)list.get(index1);
        yUnits = (String)list.get(index2);

        deltaX = (lastX - firstX) / (nPoints - 1);

        JDXDecompressor decompressor = new JDXDecompressor(tabularSpecData, xFactor, yFactor, deltaX);
        decompressor.setLabelLineNo(tabDataLineNo);

        xyCoords = decompressor.decompressData();

        if(xyCoords == null)
          xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);

        if (decompressor.getErrorLog().length()>0 ) {
          errorLog.append(decompressor.getErrorLog() + "\n");
          errorLog.append("firstX: " + firstX + " Found " +
                             (increasing ? xyCoords[0] : xyCoords[xyCoords.length -1]).getXVal()  + "\n");
          errorLog.append("lastX from Header " + lastX + " Found " +
                             (increasing ? xyCoords[xyCoords.length -1] : xyCoords[0]).getXVal()  + "\n");
          errorLog.append("deltaX from Header "+ deltaX  + "\n");
          errorLog.append("Number of points in Header "+ nPoints + " Found " + xyCoords.length  + "\n");
        }else{
          errorLog.append("No Errors\n");
        }

        if(JSpecViewUtils.DEBUG){
          System.err.println(errorLog.toString());
        }

        // apply offset
        if(offset != Graph.ERROR && obFreq != Graph.ERROR && ns.getDataType().toUpperCase().contains("SPECTRUM")){
            JSpecViewUtils.applyShiftReference(xyCoords, dataPointNum, firstX, lastX, offset, obFreq, shiftRefType);
        }

        if(obFreq != Graph.ERROR && xUnits.toUpperCase().equals("HZ")){
          double xScale = obFreq;
          JSpecViewUtils.applyScale(xyCoords, (1/xScale), 1);
          xUnits = "PPM";
          spectrum.setHZtoPPM(true);
          spectrum.setObservedFreq(obFreq);
        }
      }
      else if(spectrum.getDataClass().equals("PEAKTABLE") || spectrum.getDataClass().equals("XYPOINTS")){
        list = (ArrayList<String>)nTupleTable.get("##SYMBOL");
        int index1 = list.indexOf(plotSymbols[0]);
        int index2 = list.indexOf(plotSymbols[1]);

        list = (ArrayList<String>)nTupleTable.get("##UNITS");
        xUnits = (String)list.get(index1);
        yUnits = (String)list.get(index2);
        xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);
      }
      else
        throw new JDXSourceException("Unable to read Ntuple Source");

      spectrum.setTitle(title + " : " + page);
      spectrum.setJcampdx(ns.getJcampdx());
      spectrum.setDataType(ns.getDataType());
      spectrum.setOrigin(ns.getOrigin());
      spectrum.setOwner(ns.getOwner());

      spectrum.setXUnits(xUnits);
      spectrum.setYUnits(yUnits);
      spectrum.setXFactor(xFactor);
      spectrum.setYFactor(yFactor);

      for(Iterator<String> iter = sourceLDRTable.keySet().iterator(); iter.hasNext();){
        String key = (String)iter.next();
        if(!JSpecViewUtils.cleanLabel(key).equals("##TITLE") &&
           !JSpecViewUtils.cleanLabel(key).equals("##DATACLASS") &&
           !JSpecViewUtils.cleanLabel(key).equals("##NTUPLES"))
        LDRTable.put(key, sourceLDRTable.get(key));
      }
      spectrum.setHeaderTable(LDRTable);
      spectrum.setXYCoords(xyCoords);

      ns.addJDXSpectrum(spectrum);

      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);
      spectrum = new JDXSpectrum();
    }
    errorLog.append(errorSeparator);
    ns.setErrorLog(errorLog.toString());
    return ns;
  }

}

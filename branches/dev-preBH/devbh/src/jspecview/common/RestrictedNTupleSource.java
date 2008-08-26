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
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;

/**
 * Representation of a JCAMP-DX nTuple source. Since these source files may
 * have 128 spectra or more. Due to memory restictions only MAX_NUMBER_SPECTRA
 * of the spectra in the source will be instantiated at any one time. In order to
 * instantiate the other spectra a call to initNextSetOfSpectra() will have to be
 * made
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 */

public class RestrictedNTupleSource extends CompoundSource {

  /** The maximum number of spectra that will be instantiated */
  public static int MAX_NUMBER_SPECTRA = 32;

  // the contents of the source
  private String sourceContents;

  // The index of the set of Spectra that is currently initialised
  private int currentSpectraSetIndex = 0;

  // The number of sets in the source
  // Specified by the ceil of <number of spectra>/MAX_NUMBER_SPECTRA
  private int numberOfSets;

  /* ----------------------------------------------------------------------*/
  /*  Ntuple header and Table data */
  private ArrayList<String> attrList;

  private HashMap<String, ArrayList<String>> nTupleTable;

  private HashMap<String, String> sourceLDRTable;


  /**
   * Constructor
   * @param sourceContents the contents of the source
   */
  protected RestrictedNTupleSource(String sourceContents) {
    this.sourceContents = sourceContents;
  }

  /**
   * Returns an instance of an RestrictedNTupleSource with the first
   * MAX_NUMBER_SPECTRUMS instanciated
   * @param sourceContents the contents of the source
   * @param numberOfSpectra the number of spectra
   * @return Returns an instance of an RestrictedNTupleSource
   * @throws JSpecViewException
   */
  public static RestrictedNTupleSource getInstance(String sourceContents, int numberOfSpectra)
    throws JSpecViewException{

    RestrictedNTupleSource ns = new RestrictedNTupleSource(sourceContents);
    ns.numberOfSets = (int)Math.ceil(numberOfSpectra/MAX_NUMBER_SPECTRA);

    String tabularSpecData = null;
    JDXSpectrum spectrum;
    HashMap<String, String> LDRTable;
    ns.sourceLDRTable = new HashMap<String, String>();
    ns.nTupleTable = new HashMap<String, ArrayList<String>>();

    double offset = Graph.ERROR;
    double obFreq = Graph.ERROR;
    double firstX, lastX, xFactor = 1;
    double deltaX = 0, yFactor = 1;
    boolean continuous = true;
    int nPoints;
    String xUnits= "", title = "", yUnits = "";
    String page = "";

    int dataPointNum = -1;
    int shiftRefType = -1;

    // True if we have a long date

    String[] plotSymbols = new String[2];

    Coordinate[] xyCoords;

    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);

    // Read Source Specific Header
    String label = "";
    while(t.hasMoreTokens() && !label.equals("##NTUPLES")){
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);

      if(label.equals("##TITLE")){
        title = t.value;
        ns.setTitle(title);
        continue;
      }

      if(label.equals("##JCAMPDX")){
        ns.setJcampdx(t.value);
        continue;
      }

      if(label.equals("##ORIGIN")){
        ns.setOrigin(t.value);
        continue;
      }

      if(label.equals("##OWNER")){
        ns.setOwner(t.value);
        continue;
      }

      if (label.equals("##LONGDATE")) {
           ns.setLongDate(t.value);
           continue;
      }

      if (label.equals("##DATE"))  {
//            notesLDRTable.put(label, t.value);
            ns.setDate(t.value);
            continue;
      }

      if (label.equals("##TIME")) {
//            notesLDRTable.put(label, t.value);
            ns.setTime(t.value);
            continue;
      }

      if(label.equals("##PATHLENGTH")){
         ns.setPathlength(t.value);
         continue;
       }

      if(label.equals("##DATATYPE")){
        ns.setDataType(t.value);
        continue;
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
        StringTokenizer srt = new StringTokenizer(t.value, ",");
        if(srt.countTokens() != 4)
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
        continue;
      }

      ns.sourceLDRTable.put(label, t.value);
    } //Finished Pulling out the LDR Table Data

    ns.setHeaderTable(ns.sourceLDRTable);

    /*--------------------------------------------*/
    /*------------- Fetch Page Data --------------*/

    if(!label.equals("##NTUPLES"))
      throw new JSpecViewException("Invalid NTuple Source");


    // Read NTuple Table
    StringTokenizer st;
    while(t.hasMoreTokens()){
      t.nextToken();
      label = JSpecViewUtils.cleanLabel(t.label);
      if(label.equals("##PAGE")){
        break;
      }
      st = new StringTokenizer(t.value, ",");
      ns.attrList = new ArrayList<String>();
      while(st.hasMoreTokens()){
        ns.attrList.add(st.nextToken().trim());
      }
      ns.nTupleTable.put(label, ns.attrList);
    }//Finised With Page Data
    /*--------------------------------------------*/

    if(!label.equals("##PAGE"))
      throw new JSpecViewException("Error Reading NTuple Source");


    /*--------------------------------------------*/
    /*-------- Gather Spectra Data From File -----*/

    // Create and add Spectra
    spectrum = new JDXSpectrum();

    int setIndex = 0;
    int spectrumIndex = 0;

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

          ArrayList<String> symbols = (ArrayList<String>)ns.nTupleTable.get("##SYMBOL");
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

        list = (ArrayList<String>)ns.nTupleTable.get("##SYMBOL");
        int index1 = list.indexOf(plotSymbols[0]);
        int index2 = list.indexOf(plotSymbols[1]);

        list = (ArrayList<String>)ns.nTupleTable.get("##FACTOR");
        xFactor = Double.parseDouble((String)list.get(index1));
        yFactor = Double.parseDouble((String)list.get(index2));

        list = (ArrayList<String>)ns.nTupleTable.get("##LAST");
        lastX = Double.parseDouble((String)list.get(index1));
        //lastY = Double.parseDouble((String)list.get(index1));

        list = (ArrayList<String>)ns.nTupleTable.get("##FIRST");
        firstX = Double.parseDouble((String)list.get(index1));
        //firstY = Double.parseDouble((String)list.get(index2));

        list = (ArrayList<String>)ns.nTupleTable.get("##VARDIM");
        nPoints = Integer.parseInt((String)list.get(index1));

        list = (ArrayList<String>)ns.nTupleTable.get("##UNITS");
        xUnits = (String)list.get(index1);
        yUnits = (String)list.get(index2);

        deltaX = (lastX - firstX) / (nPoints - 1);

        JDXDecompressor decompressor = new JDXDecompressor(tabularSpecData, xFactor, yFactor, deltaX);

        xyCoords = decompressor.decompressData();

        if(xyCoords == null)
          xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);

        // apply offset
       if (offset != Graph.ERROR && obFreq != Graph.ERROR && ns.getDataType().toUpperCase().contains("SPECTRUM")) {
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
        list = (ArrayList<String>)ns.nTupleTable.get("##SYMBOL");
        int index1 = list.indexOf(plotSymbols[0]);
        int index2 = list.indexOf(plotSymbols[1]);

        list = (ArrayList<String>)ns.nTupleTable.get("##UNITS");
        xUnits = (String)list.get(index1);
        yUnits = (String)list.get(index2);
        xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);
      }
      else
        throw new JDXSourceException("Unable to read Ntuple Source");

      spectrum.setXUnits(xUnits);
      spectrum.setYUnits(yUnits);
      spectrum.setTitle(title.substring(0, (title.length() >= 20 ? 21 : title.length())) + "..." + " : " + page);

      for(Iterator<String> iter = ns.sourceLDRTable.keySet().iterator(); iter.hasNext();){
        String key = (String)iter.next();
        if(!JSpecViewUtils.cleanLabel(key).equals("##TITLE") &&
           !JSpecViewUtils.cleanLabel(key).equals("##DATACLASS") &&
           !JSpecViewUtils.cleanLabel(key).equals("##NTUPLES"))
        LDRTable.put(key, ns.sourceLDRTable.get(key));
      }
      spectrum.setHeaderTable(LDRTable);
      spectrum.setXYCoords(xyCoords);

      ns.addJDXSpectrum(spectrum);


      boolean found = false;
      while(t.hasMoreTokens()){
        if(found)
          break;
        t.nextToken();
        label = JSpecViewUtils.cleanLabel(t.label);
        if(label.equals("##PAGE")){
          spectrumIndex++;
          if((spectrumIndex % ns.numberOfSets) != setIndex)
            continue;
        }
      }

      spectrum = new JDXSpectrum();
    }

    ns.currentSpectraSetIndex++;
    return ns;

  }

  /**
   * Initialises the list of Spectra with the next Set of Spectra
   * @throws JSpecViewException
   */
  public void initNextSetOfSpectra() throws JSpecViewException{

    Vector<JDXSpectrum> jdxSpectra = this.getSpectra();
    jdxSpectra.clear();
    jdxSpectra.ensureCapacity(MAX_NUMBER_SPECTRA);


    String tabularSpecData = null;
    JDXSpectrum spectrum;
    HashMap<String, String> LDRTable;

    double offset = Graph.ERROR;
    double obFreq = Graph.ERROR;
    double firstX, lastX, xFactor = 1;
    double deltaX = 0, yFactor = 1;
    boolean continuous = true;
    int nPoints;
    String xUnits= "", title = "", yUnits = "";
    String page = "";

    int dataPointNum = -1;
    int shiftRefType = -1;
    String[] plotSymbols = new String[2];

    Coordinate[] xyCoords;

    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);

    String label = "";

    if(!hasMoreSpectra())
      return;
    int setIndex = currentSpectraSetIndex;
    int spectrumIndex = 0;


    boolean found = false;
    while(t.hasMoreTokens()){
      if(found)
        break;
      if(label.equals("##PAGE")){
        if((spectrumIndex % numberOfSets) != setIndex){
          spectrumIndex++;
          t.nextToken();
          label = JSpecViewUtils.cleanLabel(t.label);
          continue;
        }
      }
      else{
        t.nextToken();
        label = JSpecViewUtils.cleanLabel(t.label);
      }
    }


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

          ArrayList<String> symbols = (ArrayList<String>)nTupleTable.get("##SYMBOL");
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

        xyCoords = decompressor.decompressData();

        if(xyCoords == null)
          xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);

        // apply offset
        if(offset != Graph.ERROR && obFreq != Graph.ERROR){
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

      spectrum.setXUnits(xUnits);
      spectrum.setYUnits(yUnits);
      spectrum.setTitle(title.substring(0, (title.length() >= 20 ? 21 : title.length())) + "..." + " : " + page);

      for(Iterator<String> iter = sourceLDRTable.keySet().iterator(); iter.hasNext();){
        String key = (String)iter.next();
        if(!JSpecViewUtils.cleanLabel(key).equals("##TITLE") &&
           !JSpecViewUtils.cleanLabel(key).equals("##DATACLASS") &&
           !JSpecViewUtils.cleanLabel(key).equals("##NTUPLES"))
        LDRTable.put(key, sourceLDRTable.get(key));
      }
      spectrum.setHeaderTable(LDRTable);
      spectrum.setXYCoords(xyCoords);

      addJDXSpectrum(spectrum);


      found = false;
      while(t.hasMoreTokens()){
        if(found)
          break;
        t.nextToken();
        label = JSpecViewUtils.cleanLabel(t.label);
        if(label.equals("##PAGE")){
          spectrumIndex++;
          if((spectrumIndex % numberOfSets) != setIndex)
            continue;
        }
      }

      spectrum = new JDXSpectrum();
    }

    currentSpectraSetIndex++;
  }

  /**
   * Determines if the last set of Spectra have been initialised
   * and returns false
   * @return returns false if the last set of Spectra have been initialised,
   *         otherwise returns true;
   */
  public boolean hasMoreSpectra(){
    return currentSpectraSetIndex == numberOfSets ? false : true;
  }

//  /**
//   * Initalises the set of spectra at the specified index in the source
//   * @param index the index
//   */
//  protected void initSpectra(int index){
//  }

  /**
   * Returns the number of sets of Spectra in the source
   * @return the number of sets of Spectra in the source
   */
  public int getNumberOfSpectraSets(){
    return numberOfSets;
  }
}

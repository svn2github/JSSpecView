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

import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;
import jspecview.exception.TabularDataSetException;

/**
 * Representation of a JDX Simple Source.
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof. Robert J. Lancashire
 */

public class SimpleSource extends JDXSource {

  /**
   * Constructs a new SimpleSource
   * @throws JSpecViewException
   */
  protected SimpleSource() throws JSpecViewException{
    super();
  }

  /**
   * Does the actual work of initializing the SimpleSource from the
   * the contents of the source
   * @param sourceContents the contents of the source as a String
   * @return an instance of a SimpleSource
   * @throws JSpecViewException
   */
  public static SimpleSource getInstance(String sourceContents) throws JSpecViewException {

    // The SimpleSouce Instance
    SimpleSource ss = new SimpleSource();

    // Variables needed to create JDXSpectrum
    double deltaX = Graph.ERROR;
    double xFactor = Graph.ERROR;
    double yFactor = Graph.ERROR;
    double firstX = Graph.ERROR;
    double lastX = Graph.ERROR;
    int nPoints = -1;
    String xUnits = "";
    String yUnits = "";
    boolean increasing = true;
    boolean continuous = true;
    // Shift Reference for NMR
    double offset = Graph.ERROR;
    // shiftRef = 0, bruker = 1, varian = 2
    int shiftRefType = -1;
    int dataPointNum = -1;

    // Observed Frequency for NMR
    double obFreq = Graph.ERROR;

    //Calendar now = Calendar.getInstance();
    //SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS ZZZZ");
    //String currentTime =  formatter.format(now.getTime());

    String errorSeparator="________________________________________________________";

    // The data Table
    String tabularSpecData = null;
    // Array for the raw coordinates
    Coordinate[] xyCoords;

    JDXSpectrum spectrum = new JDXSpectrum();
    // Table for header information
    HashMap<String, String> notesLDRTable = new HashMap<String, String>(20);

    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);
    String label = "";
    int tabularDataLabelLineNo = 0;

    StringBuffer errorLog = new StringBuffer();

    while(t.hasMoreTokens()){
      t.nextToken();

      label = JSpecViewUtils.cleanLabel(t.label);

      if(label.equals("##END"))
        break;

      if(label.equals("##TITLE")){
        if(t.value != null && !t.value.equals(""))
          spectrum.setTitle(t.value);
        else
          spectrum.setTitle("Unknown");
        continue;
      }

      if(label.equals("##JCAMPDX")){
        if(t.value != null && !t.value.equals(""))
          spectrum.setJcampdx(t.value);
        else
          spectrum.setJcampdx("5.01");
        continue;
      }

      if(label.equals("##ORIGIN")){
        if(t.value != null && !t.value.equals(""))
          spectrum.setOrigin(t.value);
        else
          spectrum.setOrigin("Unknown");
        continue;
      }

      if(label.equals("##OWNER")){
        if(t.value != null && !t.value.equals(""))
          spectrum.setOwner(t.value);
        else
          spectrum.setOwner("Unknown");
        continue;
      }

      if (label.equals("##LONGDATE")) {
           spectrum.setLongDate(t.value);
           continue;
      }

      if (label.equals("##DATE"))  {
//            notesLDRTable.put(label, t.value);
            spectrum.setDate(t.value);
            continue;
      }

      if (label.equals("##TIME")) {
//            notesLDRTable.put(label, t.value);
            spectrum.setTime(t.value);
            continue;
      }

      if(label.equals("##PATHLENGTH")){
        spectrum.setPathlength(t.value);
        continue;
      }

      if(label.equals("##DATATYPE")){
        spectrum.setDataType(t.value);
        continue;
      }

      if(label.equals("##XLABEL")){
        xUnits = t.value;
        spectrum.setXUnits(xUnits);
        continue;
      }

      if(label.equals("##XUNITS") && xUnits.equals("")){
        if(t.value != null && !t.value.equals(""))
          xUnits = t.value;
        else
          xUnits = "Arbitrary Units";
        spectrum.setXUnits(xUnits);
        continue;
      }

      if(label.equals("##YLABEL")){
        yUnits = t.value;
        spectrum.setYUnits(yUnits);
        continue;
      }

      if(label.equals("##YUNITS") && yUnits.equals("")){
        if(t.value != null && !t.value.equals(""))
          yUnits = t.value;
        else
          yUnits = "Arbitrary Units";
        spectrum.setYUnits(yUnits);
        continue;
      }

      if(label.equals("##XFACTOR")){
        xFactor = Double.parseDouble(t.value);
        spectrum.setXFactor(xFactor);
        continue;
      }

      if(label.equals("##YFACTOR")){
        yFactor = Double.parseDouble(t.value);
        spectrum.setYFactor(yFactor);
        continue;
      }

      if(label.equals("##FIRSTX")){
        firstX = Double.parseDouble(t.value);
        //spectrum.setFirstX(firstX);
        continue;
      }

      if(label.equals("##LASTX")){
        lastX = Double.parseDouble(t.value);
        //spectrum.setLastX(lastX);
        continue;
      }

      if(label.equals("##MINX") ||
         label.equals("##MINY") ||
         label.equals("##MAXX") ||
         label.equals("##MAXY") ||
         label.equals("##FIRSTY")||
         label.equals("##DELTAX") ||
         label.equals("##DATACLASS"))
         continue;

      if(label.equals("##NPOINTS")){
        nPoints = Integer.parseInt(t.value);
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
        if (!(spectrum.getDataType().toUpperCase().contains("SPECTRUM")))
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
        notesLDRTable.put(label, t.value);
      }

      if(Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0){
        tabularDataLabelLineNo =  t.labelLineNo;

        if(label.equals("##PEAKASSIGNMENTS"))
          spectrum.setDataClass("PEAKASSIGNMENTS");

        else if(label.equals("##PEAKTABLE")){
          spectrum.setDataClass("PEAKTABLE");
          continuous = false;
        }
        else if(label.equals("##XYDATA")){
          spectrum.setDataClass("XYDATA");
        }
        else if(label.equals("##XYPOINTS")){
          spectrum.setDataClass("XYPOINTS");
        }

        //What the hell is this?
        if(spectrum.getDataClass().equals("PEAKASSIGNMENTS")){
          String tmp = t.value;
          try{
            char chr;
            do{
              tmp = tmp.substring(tmp.indexOf("\n") + 1);
              chr = tmp.trim().charAt(0);
            }
            while(chr != '(');
          }
          catch(IndexOutOfBoundsException iobe){
            throw new TabularDataSetException("Error Reading Tabular Data Set");
          }
          tabularSpecData = tmp;
        }
        else{
          String tmp = t.value;
          try{
            char chr;
            do{
              tmp = tmp.substring(tmp.indexOf("\n") + 1);
              chr = tmp.trim().charAt(0);
            }
            while(!Character.isDigit(chr) && chr != '+' && chr != '-' && chr != '.');
          }
          catch(IndexOutOfBoundsException iobe){
            throw new TabularDataSetException("Error Reading Tabular Data Set");
          }
          tabularSpecData = tmp;
        }
        continue;
      }
      notesLDRTable.put(label, t.value);
    }

    if(!label.equals("##END"))
      throw new JSpecViewException("Error Reading Data Set");

    if(tabularSpecData == null)
      throw new JSpecViewException("Error Reading Data Set");

    if(!spectrum.getDataClass().equals("PEAKASSIGNMENTS")){

      if(spectrum.getDataClass().equals("XYDATA")){

        if(xFactor == Graph.ERROR)
          throw new JSpecViewException("Error Reading Data Set: ##XFACTOR not found");

        if(yFactor == Graph.ERROR)
          throw new JSpecViewException("Error Reading Data Set: ##YFACTOR not found");

        if(firstX == Graph.ERROR)
          throw new JSpecViewException("Error Reading Data Set: ##FIRST not found");

        if(lastX == Graph.ERROR)
          throw new JSpecViewException("Error Reading Data Set: ##LASTX not found");

        if(nPoints == -1)
          throw new JSpecViewException("Error Reading Data Set: ##NPOINTS not found");


        deltaX = JSpecViewUtils.deltaX(lastX, firstX, nPoints);

        increasing = deltaX > 0 ? true : false;

        JDXDecompressor decompressor = new JDXDecompressor(tabularSpecData, xFactor, yFactor, deltaX);
        decompressor.setLabelLineNo(tabularDataLabelLineNo);
        xyCoords = decompressor.decompressData();
        if(xyCoords == null)
          xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);

        if (decompressor.getErrorLog().length()>0 ) {
          errorLog.append(decompressor.getErrorLog() +"\n");
          errorLog.append("firstX: "+ firstX +" Found "+
                             (increasing ? xyCoords[0] : xyCoords[xyCoords.length -1]).getXVal()  + "\n");
          errorLog.append("lastX from Header "+ lastX +" Found " +
                             (increasing ? xyCoords[xyCoords.length -1] : xyCoords[0]).getXVal()  + "\n");
          errorLog.append("deltaX from Header "+ deltaX  + "\n");
          errorLog.append("Number of points in Header "+ nPoints +" Found "+ xyCoords.length  + "\n");
        }else{
          errorLog.append("No Errors detected \n");
        }
        errorLog.append(errorSeparator);
        ss.setErrorLog(errorLog.toString());

        if(JSpecViewUtils.DEBUG){
          System.err.println(errorLog.toString());
        }

        // apply offset
        if (offset != Graph.ERROR && obFreq != Graph.ERROR && spectrum.getDataType().toUpperCase().contains("SPECTRUM")) {
               JSpecViewUtils.applyShiftReference(xyCoords, dataPointNum, firstX, lastX,offset, obFreq, shiftRefType);
        }

        if(obFreq != Graph.ERROR) {
          if (xUnits.toUpperCase().equals("HZ")) {
            double xScale = obFreq;
            JSpecViewUtils.applyScale(xyCoords, (1 / xScale), 1);
            spectrum.setXUnits("PPM");
            spectrum.setHZtoPPM(true);
            spectrum.setObservedFreq(obFreq);
          }
          else {
            JSpecViewUtils.applyScale(xyCoords, 1, 1);
            spectrum.setXUnits("PPM");
            spectrum.setHZtoPPM(true);
            spectrum.setObservedFreq(obFreq);
         }
        }
      }
      else if(spectrum.getDataClass().equals("PEAKTABLE") || spectrum.getDataClass().equals("XYPOINTS")){
        // check if there is an x and y factor
        if (xFactor != Graph.ERROR && yFactor != Graph.ERROR)
          xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);
        else
          xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, 1, 1);
      }
      else
        throw new JDXSourceException("Unable to read Simple Source");

      spectrum.setHeaderTable(notesLDRTable);
      spectrum.setXYCoords(xyCoords);
      spectrum.setIncreasing(increasing);
      spectrum.setContinuous(continuous);
    }

    ss.addJDXSpectrum(spectrum);
    return ss;
  }
}

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
import java.util.Calendar;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import jspecview.exception.JDXSourceException;
import jspecview.exception.JSpecViewException;
import jspecview.util.Coordinate;
import jspecview.util.JDXDecompressor;
import jspecview.util.JDXSourceStringTokenizer;
import jspecview.util.JSpecViewUtils;
/**
 * <code>BlockSource</code> class is a representation of a JCAMP-DX Block file.
 * This class is not intialised directly. Instead <code>JDXSourceFactory</code>
 * is used to determine the type of JCAMP-DX source from a stream and returns
 * an instance of the appropriate class
 * @author Debbie-Ann Facey
 * @author Khari A. Bryan
 * @author Prof Robert J. Lancashire
 * @see jspecview.common.CompoundSource
 * @see jspecview.common.NTupleSource
 * @see jspecview.common.JDXSourceFactory
 * @see jspecview.common.JDXSourceFactory#createJDXSource()
 */
public class BlockSource extends CompoundSource {

  /**
   * Creates a new Block Source
   * @throws JSpecViewException
   */
  protected BlockSource() throws JSpecViewException{
    super();
  }

  /**
   * Does the work of initializing the BlockSource from the source String
   * @param sourceContents contents of the source as a String
   * @return an instance of a BlockSource
   * @throws JSpecViewException
   */
  public static BlockSource getInstance(String sourceContents) throws JSpecViewException{

    // JDXSpectrum variables
    double deltaX = Graph.ERROR;
    double xFactor = Graph.ERROR;
    double yFactor = Graph.ERROR;
    double firstX = Graph.ERROR;
    double lastX = Graph.ERROR;
    double firstY = Graph.ERROR;
    double offset = Graph.ERROR;
    double obFreq = Graph.ERROR;
    int nPoints = -1;
    boolean increasing = true;
    boolean continuous = true;
    String xUnits = "";
    String yUnits = "";
    String tabularSpecData = null;
    String errorSeparator="________________________________________________________";

    // Type of Shift Reference for NMR Files
    int shiftRefType = -1;
    int dataPointNum = -1;

    // True if we have a long date
    boolean longDateFound = false;
    String date = "";
    String time = "";

    Coordinate[] xyCoords;

    HashMap<String,String> LDRTable;
    HashMap<String,String> sourceLDRTable = new HashMap<String,String>();
    String label, tmp;

    StringBuffer errorLog = new StringBuffer();

    BlockSource bs = new BlockSource();

    int tabDataLineNo = 0;

    JDXSpectrum spectrum;

    JDXSourceStringTokenizer t = new JDXSourceStringTokenizer(sourceContents);

    // Get the LDRs up to the ##TITLE of the first block
    t.nextToken();
    label = JSpecViewUtils.cleanLabel(t.label);
    if(!label.equals("##TITLE")){
      throw new JSpecViewException("Error Reading Source");
    }
    if(t.value != null && !t.value.equals(""))
      bs.setTitle(t.value);
    else{
      bs.setTitle("Unknown");
    }
    t.nextToken();
    label = JSpecViewUtils.cleanLabel(t.label);

    while(t.hasMoreTokens() && !label.equals("##TITLE")){

      if(label.equals("##JCAMPDX")){
        bs.setJcampdx(t.value);
      }
      else if(label.equals("##ORIGIN")){
        if(t.value != null && !t.value.equals(""))
          bs.setOrigin(t.value);
        else{
          bs.setOrigin("Unknown");
        }
      }

      else if(label.equals("##OWNER")){
        if(t.value != null && !t.value.equals(""))
          bs.setOwner(t.value);
        else{
          bs.setOwner("Unknown");
        }
      }

      else if(label.equals("##DATATYPE")){
        bs.setDataType(t.value);
      }

      else{
        sourceLDRTable.put(t.label, t.value);
      }
        t.nextToken();
        label = JSpecViewUtils.cleanLabel(t.label);

    }

    bs.setHeaderTable(sourceLDRTable);

    // If ##TITLE not found throw Exception
    if(!label.equals("##TITLE")){
      throw new JSpecViewException("Unable to Read Block Source");
    }
    spectrum = new JDXSpectrum();
    LDRTable = new HashMap<String,String>();

    try{
      while(t.hasMoreTokens()){

        if(label.equals("##JCAMPCS")){
          do{
            t.nextToken();
            label = JSpecViewUtils.cleanLabel(t.label);
          }
          while(!label.equals("##TITLE"));
          xUnits = yUnits = "";
          spectrum = new JDXSpectrum();
          continue;
        }
        else if(label.equals("##TITLE")){
          if(t.value != null && !t.value.equals(""))
            spectrum.setTitle(t.value);
          else
            spectrum.setTitle("Unknown");
        }

        else if(label.equals("##JCAMPDX")){
          spectrum.setJcampdx(t.value);
        }

        else if(label.equals("##ORIGIN")){
          if(t.value != null && !t.value.equals(""))
            spectrum.setOrigin(t.value);
          else
            spectrum.setOrigin("Unknown");
        }

        else if(label.equals("##OWNER")){
          spectrum.setOwner(t.value);
        }

        else if(label.equals("##DATATYPE")){
          spectrum.setDataType(t.value);
        }

        if (label.equals("##DATE"))  {
//            notesLDRTable.put(label, t.value);
              spectrum.setDate(t.value);
        }

        if (label.equals("##LONGDATE")) {
            spectrum.setLongDate(t.value);
            longDateFound = true;
       }

       if (label.equals("##TIME")) {
//            notesLDRTable.put(label, t.value);
             spectrum.setTime(t.value);
       }

        if(label.equals("##PATHLENGTH")){
          spectrum.setPathlength(t.value);
        }

        if(label.equals("##XLABEL")){
          xUnits = t.value;
          spectrum.setXUnits(xUnits);
        }

        if(label.equals("##XUNITS") && xUnits.equals("")){
          if(t.value != null && !t.value.equals(""))
            xUnits = t.value;
          else
            xUnits = "Arbitrary Units";
          spectrum.setXUnits(xUnits);
        }

        else if(label.equals("##YLABEL")){
          yUnits = t.value;
          spectrum.setYUnits(yUnits);
        }

        else if(label.equals("##YUNITS") && yUnits.equals("")){
          if(t.value != null && !t.value.equals(""))
            yUnits = t.value;
          else
            yUnits = "Arbitrary Units";
          spectrum.setYUnits(yUnits);
        }

        else if(label.equals("##MINX") ||  label.equals("##MINY")
             || label.equals("##MAXX") || label.equals("##MAXY")
             || label.equals("##FIRSTY") || label.equals("##DELTAX")
             || label.equals("##DATACLASS")){
          ;
        }

        else if(label.equals("##XFACTOR")){
          xFactor = Double.parseDouble(t.value);
          spectrum.setXFactor(xFactor);
        }

        else if(label.equals("##YFACTOR")){
          yFactor = Double.parseDouble(t.value);
          spectrum.setYFactor(yFactor);
        }

        else if(label.equals("##FIRSTX")){
          firstX = Double.parseDouble(t.value);
          //spectrum.setFirstX(firstX);
        }

        else if(label.equals("##LASTX")){
          lastX = Double.parseDouble(t.value);
          //spectrum.setLastX(lastX);
        }

        /*
        else if(label.equals("##FIRSTY")){
          firstY = Double.parseDouble(t.value);
          spectrum.setFirstY(firstY);
        }
        */
        else if(label.equals("##NPOINTS")){
          nPoints = Integer.parseInt(t.value);
        }

        else if( (label.equals("##$OFFSET")) && (shiftRefType != 0) )  {
           offset = Double.parseDouble(t.value);
           // bruker doesn't need dataPointNum
           dataPointNum = 1;
           // bruker type
           shiftRefType = 1;
         }

         else if( (label.equals("##$REFERENCEPOINT")) && (shiftRefType != 0) )  {
           offset = Double.parseDouble(t.value);
           // varian doesn't need dataPointNum
           dataPointNum = 1;
           // varian type
           shiftRefType = 2;
         }

        else if(label.equals("##.SHIFTREFERENCE")){
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
        }

        else if(label.equals("##.OBSERVEFREQUENCY")){
          obFreq = Double.parseDouble(t.value);
          LDRTable.put(label, t.value);
        }

        else if(Arrays.binarySearch(TABULAR_DATA_LABELS, label) > 0){
          tabDataLineNo = t.labelLineNo;

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

          // Get CoordData
          tmp = t.value;
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
        }

        // Process Block
        else if(label.equals("##END")){

          if(tabularSpecData == null)
            throw new JSpecViewException("Error Reading Data Set");

          if(!spectrum.getDataClass().equals("PEAKASSIGNMENTS")){

            if(spectrum.getDataClass().equals("XYDATA")){

              if(xFactor == Graph.ERROR)
                throw new JSpecViewException("Error Reading Data Set: ##XFACTOR not found");

              if(yFactor == Graph.ERROR)
                throw new JSpecViewException("Error Reading Data Set: ##YFACTOR not found");

              if(firstX == Graph.ERROR)
                throw new JSpecViewException("Error Reading Data Set: ##FIRSTX not found");

              if(lastX == Graph.ERROR)
                throw new JSpecViewException("Error Reading Data Set: ##LASTX not found");

              if(nPoints == -1)
                throw new JSpecViewException("Error Reading Data Set: ##NPOINTS not found");

              deltaX = JSpecViewUtils.deltaX(lastX, firstX, nPoints);

              increasing = deltaX > 0 ? true : false;

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
              if(offset != Graph.ERROR && obFreq != Graph.ERROR && !bs.getDataType().toUpperCase().contains("FID")){
                  JSpecViewUtils.applyShiftReference(xyCoords, dataPointNum, firstX, lastX, offset, obFreq, shiftRefType);
              }

              if(obFreq != Graph.ERROR && xUnits.toUpperCase().equals("HZ")){
                double xScale = obFreq;
                JSpecViewUtils.applyScale(xyCoords, (1/xScale), 1);
                spectrum.setXUnits("PPM");
                spectrum.setHZtoPPM(true);
                spectrum.setObservedFreq(obFreq);
              }
            }
            else if(spectrum.getDataClass().equals("PEAKTABLE") || spectrum.getDataClass().equals("XYPOINTS")){
              // check if there is an x and y factor
              if (xFactor != Graph.ERROR && yFactor != Graph.ERROR)
                xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, xFactor, yFactor);
              else
                xyCoords = JSpecViewUtils.parseDSV(tabularSpecData, 1, 1);

              deltaX = JSpecViewUtils.deltaX(xyCoords[xyCoords.length - 1].getXVal(), xyCoords[0].getXVal(), xyCoords.length);
              increasing = deltaX > 0 ? true : false;
            }
            else
              throw new JDXSourceException("Unable to read Block Source");

            spectrum.setHeaderTable(LDRTable);
            spectrum.setXYCoords(xyCoords);
            spectrum.setIncreasing(increasing);
            spectrum.setContinuous(continuous);
          }

          bs.addJDXSpectrum(spectrum);


          // Reset Variables
          deltaX = Graph.ERROR;
          xFactor = Graph.ERROR;
          yFactor = Graph.ERROR;
          firstX = Graph.ERROR;
          lastX = Graph.ERROR;
          firstY = Graph.ERROR;
          offset = Graph.ERROR;
          obFreq = Graph.ERROR;
          nPoints = -1;
          continuous = true;
          xUnits = "";
          yUnits = "";
          tabularSpecData = null;
          dataPointNum = -1;
          shiftRefType = -1;
          spectrum = new JDXSpectrum();
          LDRTable = new HashMap<String,String>();
        } // End Process Block

        else
          LDRTable.put(label, t.value);

        t.nextToken();
        tmp = JSpecViewUtils.cleanLabel(t.label);
        if(label.equals("##END") && tmp.equals("##END"))
          break;
        label = tmp;
      } // End Source File
    }
    catch(NoSuchElementException nsee){
      throw new JSpecViewException("Unable to Read Block Source");
    }
    catch(JSpecViewException jsve){
      throw jsve;
    }
    errorLog.append(errorSeparator);
    bs.setErrorLog(errorLog.toString());
    return bs;
  }

}

/* Copyright (c) 2006-2007 The University of the West Indies
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

package jspecview.export;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import jspecview.common.Coordinate;
import jspecview.util.Parser;
import jspecview.util.TextFormat;

/**
 * A simple Velocity-like template filler
 * -- Supports only "==" "=" "!=" and no SET
 * -- # directives must be first character of the line 
 * 
 * @author Bob Hanson, hansonr@stolaf.edu
 * 
 */
class VelocityContext {

  String[] tokens;
  Hashtable<String, Object> context = new Hashtable<String, Object>();
  Vector<VelocityToken> velocityTokens;

  VelocityContext() {
  }

  void put(String key, Object value) {
    if (value == null)
      value = "";
    context.put(key, value);
  }

  String setTemplate(String template) {
    String errMsg = getVeloctyTokens(template);
    if (errMsg != null)
      return errMsg;
    return null;
  }

  int commandLevel;
  Vector<Integer> cmds = new Vector<Integer>();
  String strError;

  final static int VT_DATA = 0;
  final static int VT_IF = 1;
  final static int VT_ELSE = 2;
  final static int VT_ELSEIF = 3;
  final static int VT_END = 4;
  final static int VT_FOREACH = 5;
  final static int VT_SET = 6;

  class VelocityToken {

    boolean hasVariable;
    int cmdType;
    int cmdPtr = -1;
    int endPtr = -1;
    int ptr;
    String var;
    Vector<Coordinate> vc;
    int velocityCount;
    String data;

    VelocityToken(String token) {
      hasVariable = token.indexOf("$") >= 0;
      data = token;
      if (token.indexOf("#") < 0) {
        velocityTokens.add(this);
        return;
      }
      ptr = velocityTokens.size();

      if (token.startsWith("#end")) {
        cmdType = VT_END;
        endPtr = ptr;
        commandLevel--;
        if (commandLevel < 0) {
          strError = "misplaced #end";
          return;
        }
        cmdPtr = cmds.remove(0).intValue();
        velocityTokens.get(cmdPtr).endPtr = ptr;
      } else {
        commandLevel++;
        if (token.startsWith("#if")) {
          cmdType = VT_IF;
          cmds.add(0, new Integer(ptr));
        } else if (token.startsWith("#foreach")) {
          cmdType = VT_FOREACH;
          cmds.add(0, new Integer(ptr));
          cmdPtr = ptr;
          if (token.indexOf("#end") > 0) {
            int pt = token.indexOf(")") + 1;
            data = token.substring(0, pt);
            velocityTokens.add(this);
            new VelocityToken(token.substring(pt, token.indexOf("#end")) + "\n");
            new VelocityToken("#end");
            return;
          }
        } else if (token.startsWith("#elseif")) {
          cmdType = VT_ELSEIF;
          cmdPtr = cmds.get(0).intValue();
          velocityTokens.get(cmdPtr).endPtr = ptr;
          cmds.add(0, new Integer(ptr));
        } else if (token.startsWith("#else")) {
          cmdType = VT_ELSE;
          cmdPtr = cmds.get(0).intValue();
          velocityTokens.get(cmdPtr).endPtr = ptr;
          cmds.add(0, new Integer(ptr));
        } else {
          System.out.println("??? " + token);
        }
      }
      velocityTokens.add(this);
    }
  }

  private String getVeloctyTokens(String template) {
    velocityTokens = new Vector<VelocityToken>();
    if (template.indexOf("\r\n") >= 0)
      template = TextFormat.replaceAllCharacters(template, "\r\n", '\n');
    template = template.replace('\r', '\n');
    String[] lines = template.split("\n");
    String token = "";
    for (int i = 0; i < lines.length && strError == null; i++) {
      String line = TextFormat.rtrim(lines[i], " \n");
      if (line.length() == 0)
        continue;
      if (line.indexOf("#") == 0) {
        if (token.length() > 0) {
          new VelocityToken(token + "\n");
          token = "";
        }
        if (strError != null)
          break;
        new VelocityToken(line);
        continue;
      }
      token += line + "\n";
    }
    if (token.length() > 0 && strError == null) {
      new VelocityToken(token);
    }
    return strError;
  }

  public String merge(FileWriter writer) {
    int ptr;
    for (int i = 0; i < velocityTokens.size() && strError == null; i++) {
      VelocityToken vt = velocityTokens.get(i);
      //System.out.println(i + " " + vt.cmdType + " " + vt.cmdPtr + " "
        //  + vt.endPtr  + vt.data);
      try {
        switch (vt.cmdType) {
        case VT_DATA:
          String data = fillData(vt.data);
          writer.write(data);
          //System.out.println(data);
          continue;
        case VT_IF:
          if (evaluate(vt.data, true)) {
            vt.endPtr = -vt.endPtr;
          } else {
            i = vt.endPtr - 1;
          }
          continue;
        case VT_ELSE:
        case VT_ELSEIF:
          if ((ptr = velocityTokens.get(vt.cmdPtr).endPtr) < 0) {
            // previous block was executed -- skip to end
            velocityTokens.get(vt.cmdPtr).endPtr = -ptr;
            while ((vt = velocityTokens.get(vt.endPtr)).cmdType != VT_END) {
              // skip 
            }
            i = vt.ptr;
            continue;
          }
          if (vt.cmdType == VT_ELSEIF) {
            if (evaluate(vt.data, true)) {
              vt.endPtr = -vt.endPtr;
            } else {
              i = vt.endPtr - 1;
            }
          }
          continue;
        case VT_FOREACH:
          foreach(vt);
        // fall through
        case VT_END:
          if ((vt = velocityTokens.get(vt.cmdPtr)).cmdType != VT_FOREACH)
            continue;
          if (vt.vc == null)
            continue;
          if (++vt.velocityCount == vt.vc.size()) {
            i = vt.endPtr;
            continue;
          }
          Coordinate c = vt.vc.elementAt(vt.velocityCount);
          context.put("velocityCount", new Integer(vt.velocityCount));
          context.put(vt.var + ".xVal", new Double(c.getXVal()));
          context.put(vt.var + ".yVal", new Double(c.getYVal()));
          context.put(vt.var + ".getXString()", c.getXString());
          context.put(vt.var + ".getYString()", c.getYString());
          i = vt.cmdPtr;
          continue;
        case VT_SET:
          set(vt.data);
          continue;
        }
      } catch (IOException e) {
        return e.getMessage();
      }
    }
    return strError;
  }

  @SuppressWarnings("unchecked")
  private void foreach(VelocityToken vt) {
    String data = vt.data;
    data = data.replace('(', ' ');
    data = data.replace(')', ' ');
    String[] tokens = Parser.getTokens(data);
    if (tokens.length != 4) {
      return;
    }
    // #foreach  $xxx in XXX 
    vt.var = tokens[1].substring(1);
    Object vc = context.get(tokens[3].substring(1));
    if (vc instanceof Vector)
      vt.vc = (Vector<Coordinate>) vc;
    vt.cmdPtr = vt.ptr;
    vt.velocityCount = -1;
  }

  private void set(String data) {
    // TODO

  }

  private final static String[] ops = { "==", "!=", "=" };
  private final static int OP_EEQ = 0;
  private final static int OP_NE = 1;
  private final static int OP_EQ = 2;

  private static int findOp(String op) {
    for (int i = ops.length; --i >= 0;)
      if (ops[i].equals(op))
        return i;
    return -1;
  }

  private boolean evaluate(String data, boolean isIf) {
    int pt = data.indexOf("(");
    if (pt < 0) {
      strError = "missing ( in " + data;
      return false;
    }
    data = data.substring(pt + 1);
    pt = data.lastIndexOf(")");
    if (pt < 0) {
      strError = "missing ) in " + data;
      return false;
    }
    data = data.substring(0, pt);
    data = TextFormat.simpleReplace(data, "=", " = ");
    data = TextFormat.simpleReplace(data, "!", " ! ");
    data = TextFormat.simpleReplace(data, "<", " < ");
    data = TextFormat.simpleReplace(data, ">", " > ");
    data = TextFormat.simpleReplace(data, "=  =", "==");
    data = TextFormat.simpleReplace(data, "<  =", "<=");
    data = TextFormat.simpleReplace(data, ">  =", ">=");
    data = TextFormat.simpleReplace(data, "!  =", "!=");
    String[] tokens = Parser.getTokens(data);
    String key = tokens[0].substring(1);
    boolean isNot = false;
    boolean x = false;
    String value = null;
    String compare = "";
    try {
    switch (tokens.length) {
    case 1:
      // #if($x)
      value = getValue(key);
      return (!value.equals("") && !value.equals("false"));
    case 2:
      // #if(!$x)
      if (key.equals("!")) {
        key = TextFormat.trim(tokens[1], "$ ");
        value = getValue(key);
        return (value.equals("false") || value.equals(""));
      }
      break;
    case 3:
      // #if($x op "y")
      key = TextFormat.trim(tokens[0], "$ ");
      value = getValue(key);
      compare = TextFormat.trim(tokens[2], " \"");
      switch (findOp(tokens[1])) {
      case OP_EQ:
      case OP_EEQ:
        return (value.equals(compare));
      case OP_NE:
        return (!value.equals(compare));
      default:
        System.out.println("???? " + key + " " + compare + " " + value);
      }
      break;
    }
    } catch (Exception e) {
      System.out.println(e.getMessage() + " in VelocityContext.merge");
    }
//    if (value != null) {
  //    x = !value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("0");
    //}
    return isNot ? !x : x;
  }

  private String getValue(String key) {
    return (context.containsKey(key) ? context.get(key).toString() : "");
  }
  
  private String fillData(String data) {
    int i = 0;
    int ccData = data.length();
    while (i < ccData) {
      while (i < ccData && data.charAt(i++) != '$') {
        // continue looking for start
      }
      if (i == ccData)
        break;
      int j = i;
      char ch;
      while (++j < ccData
          && (Character.isLetterOrDigit(ch = data.charAt(j)) || ch == '.' || ch == '_')) {
        // continue looking for end        
      }
      if (j < ccData && data.charAt(j) == '(')
        j += 2;
      String key = data.substring(i, j);
      if (context.containsKey(key)) {
        Object value = context.get(key);
        String strValue;
        if (value instanceof Coordinate) {
          strValue = value.toString();
        } else {
          strValue = value.toString();
        }
        //System.out.println(key + " = " + value);
        data = data.substring(0, i - 1) + strValue + data.substring(j);
        ccData = data.length();
        i += strValue.length();
      }
    }
    return data;
  }

}

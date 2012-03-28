package jspecview.common;

import java.util.StringTokenizer;

import jspecview.util.TextFormat;

public class ScriptCommandTokenizer extends StringTokenizer {

  public ScriptCommandTokenizer(String str, String delim) {
    super(str, delim);
  }

  static String nextStringToken(StringTokenizer params,
                                        boolean removeQuotes) {
    String s = params.nextToken();
    if (s.charAt(0) != '"')
      return s;
    if (s.endsWith("\"") && s.length() > 1)
      return (removeQuotes ? TextFormat.trimQuotes(s) : s);
    StringBuffer sb = new StringBuffer(s.substring(1));
    s = null;
    while (params.hasMoreTokens() && !(s = params.nextToken()).endsWith("\"")) {
      sb.append(" ").append(s);
      s = null;
    }
    if (s != null)
      sb.append(" ").append(s.substring(0, s.length() - 1));
    s = sb.toString();
    return (removeQuotes ? s : "\"" + s + "\"");
  }

  @Override
  public String nextToken() {
    StringBuffer sb = new StringBuffer();
    boolean inQuotes = false;
    int pt = 0;
    while (hasMoreTokens()) {
      sb.append(super.nextToken());
      for (int i = sb.length(); --i >= pt;)
        if (sb.charAt(i) == '\"')
          inQuotes = !inQuotes;
      if (!inQuotes || !hasMoreTokens())
        break;
      pt = sb.length();
      sb.append(";");
    }
    return sb.toString().trim();
  }

}

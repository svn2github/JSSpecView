/*
*  JSpecView Utility functions
*  Version 1.0, Copyright(c) 2005, Dept of Chemistry, University of the West Indies, Mona
*  Robert J Lancashire  robert.lancashire@uwimona.edu.jm
*/
var _JSVversionnumber=1.2;

/*
	Inserts the JSpecView applet in any compatible User Agent using the <object> tag
	uses IE conditional comments to distinguish between IE and Mozilla
	see http://msdn.microsoft.com/workshop/author/dhtml/overview/ccomment_ovw.asp
*/
function insertJSVObject(_JSVarchive,_JSVtarget,_JSVwidth,_JSVheight,_JSVscript){


// script for Mozilla
_JSVMozscript='<object classid="java:jspecview.applet.JSVApplet.class" '
              +'type="application/x-java-applet;version=1.4" archive= "'+ _JSVarchive+'" '
		  +'id= "'+_JSVtarget
		  +'" height="'+_JSVheight
		  +'" width="'+_JSVwidth+'" >\n'
 	 +'<param name="script" value="'+_JSVscript +'" />\n'
	 +'<param name="mayscript" value="true" />\n';

// script for MSIE (Microsoft Internet Explorer) and SUN plugin version 1.4 at least
_JSVIEscript='<object classid="clsid:8AD9C840-044E-11D1-B3E9-00805F499D93" \n'
	       +'codebase="http://java.sun.com/products/plugin/autodl/jinstall-1_4-windows-i586.cab#Version=1,4,0,0" \n'
		 +'id= "'+_JSVtarget
		 +'" height="'+_JSVheight
		 +'" width="'+_JSVwidth+'" >\n'
         +'<param name="code" value="jspecview.applet.JSVApplet.class" />\n'
	 +'<param name="archive" value="'+_JSVarchive+'" />\n'
      	 +'<param name="script" value="'+_JSVscript +'" />\n'
	 +'<param name="scriptable" value="true" />\n'
	 +'<param name="mayscript" value="true" />\n';

// else no Sun Java Plug-in available or non-compatible UA's ?
_JSVerror='<strong>This browser does not have a Java Plug-in or needs upgrading.<br />'+
            '<a href="http://java.sun.com/products/plugin/downloads/index.html">'+
      	    'Get the latest Sun Java Plug-in from here.</a>'+'<\/strong>';

// edit only the lines above here
// do not remove the comments or the script will fail to work properly!

document.write("<!--[if !IE]> Mozilla and others will this use outer object -->");
document.write(_JSVMozscript);
document.write("<!--<![endif]-->");
document.write("<!-- MSIE (Microsoft Internet Explorer) will use the inner object -->");
document.write(_JSVIEscript);
document.write(_JSVerror);
document.write("<\/object>");
document.write("<!--[if !IE]> close outer object -->");
document.write("<\/object>");
document.write("<!--<![endif]-->");

// end of function
}

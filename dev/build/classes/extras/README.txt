
.:.............................JSpecView.....................................:.

JSpecView is an open-source viewer for JCAMP-DX files written in Java and 
requires 1.5+   It was originally developed at:
The Department of Chemistry, University of the West Indies, Mona, JAMAICA.

Examples can be seen at jspecview.sf.net

Usage questions/comments should be posted to jspecview-users@lists.sourceforge.net


Files list:
-----------
-COPYRIGHT.txt: Copyright information
-LICENSE.txt: GNU LGPL.
-README.txt: this file.

.:. applet .:.

-jspecview.jar: JSpecView Applet for use in web pages.
-JSVfunctions.js: JavaScript Library to simplify development of web pages.
-plot.vm: required XML template for export of SVG files with reverse plot function etc.
-animl_tmp.vm required XML template for export of AnIML UV/Vis/IR files
-animl_nmr.vm required XML template for export of AnIML 1D NMR files
-cml_tmp.vm required XML template for export of CML UV/Vis/IR files
-cml_nmr.vm required XML template for export of CML 1D NMR files

.:. application .:.

-JSVApp.jar: Standalone application.
-displaySchemes.dtd:
-displaySchemes.xml: required display schemes that can be modified to effect font and colour changes
-plot.vm: required XML template for export of SVG files with reverse plot function etc.
-animl_tmp.vm required XML template for export of AnIML UV/Vis/IR files
-animl_nmr.vm required XML template for export of AnIML 1D NMR files
-cml_tmp.vm required XML template for export of CML UV/Vis/IR files
-cml_nmr.vm required XML template for export of CML 1D NMR files
-sample.jdx: required for Options dialog box to show examples of colours and fonts

-pclanilIR.xml: sample AnIML file exported from
-pclanilIR.jdx for testing reading and writing IR files

The application will not function correctly if the displaySchemes can not be found.
This can happen if you try to run the JAR from the desktop. 
You should place the files listed above in a directory 
and make a shortcut to place on the desktop instead.



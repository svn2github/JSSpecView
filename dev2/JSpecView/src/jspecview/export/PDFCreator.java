/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2009-06-30 18:58:33 -0500 (Tue, 30 Jun 2009) $
 * $Revision: 11158 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Map;

import javajs.api.GenericColor;
import javajs.awt.Font;
import javajs.util.ColorUtil;
import javajs.util.List;
import javajs.util.SB;


import jspecview.api.JSVGraphics;
import jspecview.api.JSVPanel;
import jspecview.api.PdfCreatorInterface;
import jspecview.common.PrintLayout;

public class PDFCreator implements PdfCreatorInterface, JSVGraphics {
  
	private PrintLayout pl;
	private OutputStream os;
	private List<PDFObject> indirectObjects;
	private PDFObject root;
	private PDFObject graphics;	
//	private PDFObject pageResources;
//	private PDFObject graphicsResources;

	private int pt;
	private int xrefPt;
	private int count;
  private boolean isLandscape;

	private int height;
	private int width;
	
	private Map<String, PDFObject>fonts;
	private JSVGraphics g2d;
	private Font currentFont;

	public PDFCreator() {
   // for Class.forName  
  }

	public void createPdfDocument(JSVPanel panel, PrintLayout pl, OutputStream os) {
		isLandscape = pl.layout.equals("landscape");
		this.pl = pl;
		this.os = os;
		g2d = panel.getPanelData().g2d;
		try {
			width = (isLandscape ? pl.paperHeight : pl.paperWidth);
			height = (isLandscape ? pl.paperWidth : pl.paperHeight);
			System.out.println("Creating PDF with width=" + width + " and height=" + height);
			newDocument();
			panel.printPdf(this, pl);
			closeDocument();
		} catch (Exception e) {
			e.printStackTrace();
			panel.showMessage(e.toString(), "PDF Creation Error");
		}
	}

	private void newDocument() {
		fonts = new Hashtable<String, PDFObject>();
		indirectObjects = new List<PDFObject>();
		//graphicsResources = newObject(null);
		//pageResources = newObject(null); // will set this to compressed stream later
		root = newObject("Catalog");
		PDFObject pages = newObject("Pages");
		PDFObject page = newObject("Page");
		PDFObject pageContents = newObject(null);
		graphics = newObject("XObject");
		
		root.addDef("Pages", pages.getRef());
		pages.addDef("Count", "1");
		pages.addDef("Kids", "[ " + page.getRef() +" ]");
		page.addDef("Parent", pages.getRef());
		page.addDef("MediaBox", "[ 0 0 " + pl.paperWidth + " " + pl.paperHeight + " ]");
		if (isLandscape)
			page.addDef("Rotate", "90");

		pageContents.addDef("Length", "?");
		pageContents.append((isLandscape ? "q 0 1 1 0 0 0 " : "q 1 0 0 -1 0 "+(pl.paperHeight))+" cm /" + graphics.getID() + " Do Q");
		page.addDef("Contents", pageContents.getRef());		
		addProcSet(page);
		addProcSet(graphics);
		// will add fonts as well as they are needed
		graphics.addDef("Subtype", "/Form");
		graphics.addDef("FormType", "1");
		graphics.addDef("BBox", "[0 0 " + width + " " + height + "]");
		graphics.addDef("Matrix", "[1 0 0 1 0 0]");
		graphics.addDef("Length", "?");
		page.addResource("XObject", graphics.getID(), graphics.getRef());
		
		g("q 1 w 1 J 1 j 10 M []0 d q "); // line width 1, line cap circle, line join circle, miter limit 10, solid
		clip(0, 0, width, height);
	}		

	private void addProcSet(PDFObject o) {
		o.addResource(null, "ProcSet", "[/PDF /Text /ImageB /ImageC /ImageI]");
	}

	private void clip(int x1, int y1, int x2, int y2) {
		moveto(x1, y1);
		lineto(x2, y1);
		lineto(x2, y2);
		lineto(x1, y2);
		g("h W n");
	}

	private void moveto(int x, int y) {
		g(x + " " + y  + " m");
	}

	private void lineto(int x, int y) {
		g(x + " " + y  + " l");
	}

	private PDFObject newObject(String type) {
		PDFObject o = new PDFObject(++count);
		if (type != null)
			o.addDef("Type", "/" + type);
		indirectObjects.addLast(o);
		return o;
	}

	private PDFObject addFont(String fname) {
		PDFObject f = newObject("Font");
		fonts.put(fname, f);
		f.addDef("BaseFont", fname);
		f.addDef("Encoding", "/WinAnsiEncoding");
		f.addDef("Subtype", "/Type1");
		graphics.addResource("Font", f.getID(), f.getRef());
		return f;
	}

	private Map<Object, PDFObject> images;
	
	private void addImage(Object newImage, int width, int height, int[] buffer, boolean isColored) {
		if (images == null)
			images = new Hashtable<Object, PDFObject>();
		PDFObject imageObj = newObject("XObject");
		imageObj.addDef("Subtype", "/Image");
		imageObj.addDef("Length", "?");
		imageObj.addDef("ColorSpace", isColored ? "/DeviceRGB" : "/DeviceGray");
		imageObj.addDef("BitsPerComponent", "8");
		imageObj.addDef("Width", "" + width);
		imageObj.addDef("Height", "" + height);
		graphics.addResource("XObject", imageObj.getID(), imageObj.getRef());
		int n = buffer.length;
		byte[] stream	= new byte[n * (isColored ? 3 : 1)];
		if (isColored) {
			for (int i = 0, pt = 0; i < n; i++) {
				stream[pt++] = (byte) ((buffer[i] >> 16) & 0xFF);
				stream[pt++] = (byte) ((buffer[i] >> 8) & 0xFF);
				stream[pt++] = (byte) (buffer[i] & 0xFF);
			}
		} else {
			for (int i = 0; i < n; i++)
				stream[i] = (byte) buffer[i];
		}
		imageObj.setStream(stream);
		graphics.addResource("XObject", imageObj.getID(), imageObj.getRef());
		images.put(newImage, imageObj);		
	}

	private void g(String cmd) {
		graphics.append(cmd).appendC('\n');
	}

	private void output(String s) throws IOException {
	 byte[] b = s.getBytes();
	 os.write(b, 0, b.length);
	 pt += b.length;
	}

	private void closeDocument() throws IOException {
		g("Q Q");
		outputHeader();
		writeObjects();
		writeXRefTable();
		writeTrailer();
		os.flush();
		os.close();
	}

	private void outputHeader() throws IOException {
		output("%PDF-1.3\n%");
		byte[] b = new byte[] {-1, -1, -1, -1};
		os.write(b, 0, b.length);
		pt += 4;
		output("\n");
	}

	private void writeTrailer() throws IOException {
		PDFObject trailer = new PDFObject(-2);
		output("trailer");
		trailer.addDef("Size", "" + indirectObjects.size());
		trailer.addDef("Root", root.getRef());
		trailer.output(os);
		output("startxref\n");
		output("" + xrefPt + "\n");
		output("%%EOF\n");
	}

	/**
	 * Write Font objects first.
	 * 
	 * @throws IOException
	 */
	private void writeObjects() throws IOException {
		int nObj = indirectObjects.size();
		for (int i = 0; i < nObj; i++) {
			PDFObject o = indirectObjects.get(i);
			if (!o.isFont())
				continue;
			o.pt = pt;
			pt += o.output(os);
		}
		for (int i = 0; i < nObj; i++) {
			PDFObject o = indirectObjects.get(i);
			if (o.isFont())
				continue;
			o.pt = pt;
			pt += o.output(os);
		}
	}

	private void writeXRefTable() throws IOException {
		xrefPt = pt;
		int nObj = indirectObjects.size();
		SB sb = new SB();
		// note trailing space, needed because \n is just one character
		sb.append("xref\n0 " + (nObj + 1) 
				+ "\n0000000000 65535 f\r\n");
		for (int i = 0; i < nObj; i++) {
			PDFObject o = indirectObjects.get(i);
			String s = "0000000000" + o.pt;
			sb.append(s.substring(s.length() - 10));
			sb.append(" 00000 n\r\n");
		}
		output(sb.toString());
	}

	public boolean canDoLineTo() {
		return true;
	}

	private boolean inPath;
	
	public void doStroke(Object g, boolean isBegin) {
		 inPath = isBegin;
		 if (!isBegin)
			 g("S");		
	}

	public void drawCircle(Object g, int x, int y, int diameter) {
		bezierCircle(x, y, diameter/2.0, false);		
	}

	private void bezierCircle(int x, int y, double r, boolean doFill) {
		double d = r*4*(Math.sqrt(2)-1)/3;
		double dx = x;
		double dy = y;
		g((dx + r) + " " + dy + " m");
		g((dx + r) + " " + (dy + d) + " " + (dx + d) + " " + (dy + r) + " " + (dx) + " " + (dy + r) + " "  + " c");
		g((dx - d) + " " + (dy + r) + " " + (dx - r) + " " + (dy + d) + " " + (dx - r) + " " + (dy) + " c");
		g((dx - r) + " " + (dy - d) + " " + (dx - d) + " " + (dy - r) + " " + (dx) + " " + (dy - r) + " c");
		g((dx + d) + " " + (dy - r) + " " + (dx + r) + " " + (dy - d) + " " + (dx + r) + " " + (dy) + " c");
		g(doFill ? "f" : "h S");
	}

	public void drawLine(Object g, int x0, int y0, int x1, int y1) {
		moveto(x0, y0);
		lineto(x1, y1);
		if (!inPath)
			g("S");		
	}

	public void drawPolygon(Object g, int[] axPoints, int[] ayPoints, int nPoints) {
		moveto(axPoints[0], ayPoints[0]);
		for (int i = 1; i < nPoints; i++)
		  lineto(axPoints[i], ayPoints[i]);
		g("s");
	}

	public void drawRect(Object g, int x, int y, int width, int height) {
		g(x + " " + y + " " + width + " " + height + " re s");
	}

	public void drawString(Object g, String s, int x, int y) {
		drawStringRotated(g, s, x, y, 0);
	}

	public void drawStringRotated(Object g, String s, int x, int y, double angle) {
		angle = angle / 180.0 * Math.PI;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		if (Math.abs(cos) < 0.0001)
			cos = 0;
		if (Math.abs(sin) < 0.0001)
			sin = 0;
		g("q " + cos + " " + sin + " " + sin + " " + -cos + " " + x + " " + y + " cm BT(" + s + ")Tj ET Q");
	}
	
	public void fillBackground(Object g, GenericColor bgcolor) {
		// n/a?
	}

	public void fillCircle(Object g, int x, int y, int diameter) {
		bezierCircle(x, y, diameter/2.0, true);				
	}

	public void fillPolygon(Object g, int[] ayPoints, int[] axPoints, int nPoints) {
		moveto(axPoints[0], ayPoints[0]);
		for (int i = 1; i < nPoints; i++)
		  lineto(axPoints[i], ayPoints[i]);
		g("f");
	}

	public void fillRect(Object g, int x, int y, int width, int height) {
		g(x + " " + y + " " + width + " " + height + " re f");
	}

	public void lineTo(Object g, int x, int y) {
		lineto(x, y);
	}

	private static float[] rgb = new float[3];
	public void setGraphicsColor(Object g, GenericColor c) {
		ColorUtil.toRGBf(c.getRGB(), rgb);
		g(rgb[0] + " " + rgb[1] + " " + rgb[2] + " rg");
		g(rgb[0] + " " + rgb[1] + " " + rgb[2] + " RG");
	}

	public void setGraphicsFont(Object g, Font font) {
		currentFont = font;
		String fname = "/Helvetica";// + font.fontFace;
		switch (font.idFontStyle) {
		case Font.FONT_STYLE_BOLD:
			fname += "-Bold";
			break;
		case Font.FONT_STYLE_BOLDITALIC:
			fname += "-BoldOblique";
			break;
		case Font.FONT_STYLE_ITALIC:
			fname += "-Oblique";
			break;
		}
		PDFObject f = fonts.get(fname);
		if (f == null)
			f = addFont(fname);
		g("/" + f.getID() + " " + font.fontSizeNominal + " Tf");
		
	}

	public void setStrokeBold(Object g, boolean tf) {
		g((tf ? 2 : 1) + " w");		
	}

	public void translateScale(Object g, double x, double y, double scale) {
		g(scale + " 0 0 " + scale + " " + x + " " + y + " cm");
	}

	
	public Object newGrayScaleImage(Object g, Object image, int width,
			int height, int[] buffer) {
		addImage(image, width, height, buffer, false);
		return image;
	}

	public void drawGrayScaleImage(Object g, Object image, int destX0, int destY0,
			int destX1, int destY1, int srcX0, int srcY0, int srcX1, int srcY1) {
		PDFObject imageObj = images.get(image);
		if (imageObj == null)
			return;
		g("q");
		clip(destX0, destY0, destX1, destY1);
		float iw = Float.parseFloat((String) imageObj.getDef("Width"));
		float ih = Float.parseFloat((String) imageObj.getDef("Height"));		
		float dw = (destX1 - destX0 + 1);
		float dh  = (destY1 - destY0 + 1);
		float sw = (srcX1 - srcX0 + 1);
		float sh = (srcY1 - srcY0 + 1);
		float scaleX = dw / sw;
		float scaleY = dh / sh;
		float transX = destX0 - srcX0 * scaleX;
		float transY = destY0 + (ih - srcY0) * scaleY;
		g(scaleX*iw + " 0 0 " + -scaleY*ih + " " + transX + " " + transY + " cm");
		g("/" + imageObj.getID() + " Do");
		g("Q");
	}

	////////// defer to JsG2D ///////
	
	public void setWindowParameters(int width, int height) {
		// n/a
	}

	public GenericColor getColor1(int argb) {
		return g2d.getColor1(argb);
	}

	public GenericColor getColor3(int red, int green, int blue) {
		return g2d.getColor3(red, green, blue);
	}

	public GenericColor getColor4(int r, int g, int b, int a) {
		return g2d.getColor4(r, g, b, a);
	}

	public int getFontHeight(Object g) {
		return currentFont.getAscent();
	}

	public int getStringWidth(Object g, String s) {
		return currentFont.stringWidth(s);
	}


}

package jspecview.api;

import java.io.OutputStream;

import jspecview.java.AwtPanel;
import jspecview.java.PrintLayout;

public interface PdfCreatorInterface {

	void createPdfDocument(AwtPanel awtPanel, PrintLayout pl, OutputStream os);
}

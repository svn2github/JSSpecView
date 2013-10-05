package jspecview.api;

import java.io.OutputStream;

import jspecview.common.PrintLayout;
import jspecview.java.AwtPanel;

public interface PdfCreatorInterface {

	void createPdfDocument(AwtPanel awtPanel, PrintLayout pl, OutputStream os);
}

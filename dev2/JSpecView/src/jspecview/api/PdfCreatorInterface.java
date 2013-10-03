package jspecview.api;

import java.io.OutputStream;

import jspecview.common.AwtPanel;
import jspecview.common.PrintLayout;

public interface PdfCreatorInterface {

	void createPdfDocument(AwtPanel awtPanel, PrintLayout pl, OutputStream os);
}

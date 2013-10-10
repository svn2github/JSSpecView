package jspecview.api;

import java.io.OutputStream;

import jspecview.common.PrintLayout;

public interface PdfCreatorInterface {

	void createPdfDocument(JSVPanel panel, PrintLayout pl, OutputStream os);
}

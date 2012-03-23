package jspecview.common;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.List;

import jspecview.util.Escape;
import jspecview.util.Logger;

public class JSVDropTargetListener implements DropTargetListener {

  private ScriptInterface si;
  private boolean allowAppend;

  public JSVDropTargetListener(ScriptInterface si, boolean allowAppend) {
    this.si = si;
    this.allowAppend = allowAppend;
    
    // TODO Auto-generated constructor stub
  }

  //
  //   Abstract methods that are used to perform drag and drop operations
  //

  public void dragEnter(DropTargetDragEvent dtde) {
    // Called when the user is dragging and enters this drop target.
    // accept all drags
    dtde.acceptDrag(dtde.getSourceActions());
  }

  public void dragOver(DropTargetDragEvent dtde) {
  }

  public void dragExit(DropTargetEvent dtde) {
  }

  public void dropActionChanged(DropTargetDragEvent dtde) {
    // Called when the user changes the drag action between copy or move
  }

  // Called when the user finishes or cancels the drag operation.
  @SuppressWarnings("unchecked")
  public void drop(DropTargetDropEvent dtde) {
    Logger.debug("Drop detected...");
    Transferable t = dtde.getTransferable();
    boolean isAccepted = false;
    // idea here is that if the drop is into the panel ('this'), then
    // we want a replacement; if the drop is to the menu, then we want an addition.
    // just an idea....
    boolean doAppend = (allowAppend && dtde.getDropTargetContext().getDropTarget()
        .getComponent() != si);
    String prefix = (doAppend ? "" : "close ALL;");
    String postfix = (doAppend ? "" : "overlay ALL");
    String cmd = (doAppend? "LOAD APPEND " : "LOAD ");
    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      while (true) {
        Object o = null;
        try {
          dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          o = t.getTransferData(DataFlavor.javaFileListFlavor);
          isAccepted = true;
        } catch (Exception e) {
          Logger.error("transfer failed");
        }
        // if o is still null we had an exception
        if (o instanceof List) {
          List<File> list = (List<File>) o;
          dtde.getDropTargetContext().dropComplete(true);
          File[] files = (File[]) list.toArray();
          dtde = null;
          StringBuffer sb = new StringBuffer(prefix);
          for (int i = 0; i < list.size(); i++)
            sb.append(cmd
                + Escape.escape(files[i].getAbsolutePath()) + ";\n");
          sb.append(postfix);
          si.runScript(sb.toString());
          /*          
                    
                    
                    final int length = fileList.size();
                    if (length == 1) {
                      String fileName = fileList.get(0).getAbsolutePath().trim();
                      if (fileName.endsWith(".bmp"))
                        break; // try another flavor -- Mozilla bug
                      dtde.getDropTargetContext().dropComplete(true);
                      loadFile(fileName);
                      return;
                    }
          */
          return;
        }
        break;
      }
    }

    Logger.debug("browsing supported flavours to find something useful...");
    DataFlavor[] df = t.getTransferDataFlavors();

    if (df == null || df.length == 0)
      return;
    for (int i = 0; i < df.length; ++i) {
      DataFlavor flavor = df[i];
      Object o = null;
      if (true) {
        Logger.info("df " + i + " flavor " + flavor);
        Logger.info("  class: " + flavor.getRepresentationClass().getName());
        Logger.info("  mime : " + flavor.getMimeType());
      }

      if (flavor.getMimeType().startsWith("text/uri-list")
          && flavor.getRepresentationClass().getName().equals(
              "java.lang.String")) {

        /*
         * This is one of the (many) flavors that KDE provides: df 2 flavour
         * java.awt.datatransfer.DataFlavor[mimetype=text/uri-list;
         * representationclass=java.lang.String] java.lang.String String: file
         * :/home/egonw/data/Projects/SourceForge/Jmol/Jmol-HEAD/samples/
         * cml/methanol2.cml
         * 
         * A later KDE version gave me the following. Note the mime!! hence the
         * startsWith above
         * 
         * df 3 flavor java.awt.datatransfer.DataFlavor[mimetype=text/uri-list
         * ;representationclass=java.lang.String] class: java.lang.String mime :
         * text/uri-list; class=java.lang.String; charset=Unicode
         */

        try {
          o = null;
          if (!isAccepted)
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          isAccepted = true;
          o = t.getTransferData(flavor);
        } catch (Exception e) {
          Logger.error(null, e);
        }

        if (o instanceof String) {
          dtde.getDropTargetContext().dropComplete(true);
          if (Logger.debugging) {
            Logger.debug("  String: " + o.toString());
          }
          si.runScript(prefix + cmd + Escape.escape(o.toString()) + "\";" + postfix);
          return;
        }
      } else if (flavor.getMimeType().equals(
          "application/x-java-serialized-object; class=java.lang.String")) {

        /*
         * This is one of the flavors that jEdit provides:
         * 
         * df 0 flavor java.awt.datatransfer.DataFlavor[mimetype=application/
         * x-java-serialized-object;representationclass=java.lang.String] class:
         * java.lang.String mime : application/x-java-serialized-object;
         * class=java.lang.String String: <molecule title="benzene.mol"
         * xmlns="http://www.xml-cml.org/schema/cml2/core"
         * 
         * But KDE also provides:
         * 
         * df 24 flavor java.awt.datatransfer.DataFlavor[mimetype=application
         * /x-java-serialized-object;representationclass=java.lang.String]
         * class: java.lang.String mime : application/x-java-serialized-object;
         * class=java.lang.String String: file:/home/egonw/Desktop/1PN8.pdb
         */

        try {
          o = null;
          if (!isAccepted)
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
          isAccepted = true;
          o = t.getTransferData(df[i]);
        } catch (Exception e) {
          Logger.error(null, e);
        }

        if (o instanceof String) {
          String content = (String) o;
          dtde.getDropTargetContext().dropComplete(true);
          if (Logger.debugging) {
            Logger.debug("  String: " + content);
          }
          if (content.startsWith("file:/")) {
            si.runScript(prefix + cmd + Escape.escape(content) + "\";" + postfix);
          }
          return;
        }
      }
    }
    if (!isAccepted)
      dtde.rejectDrop();
  }


  
}

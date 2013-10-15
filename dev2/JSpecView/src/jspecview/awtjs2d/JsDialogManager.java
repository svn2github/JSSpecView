package jspecview.awtjs2d;

import jspecview.api.JSVPanel;
import jspecview.dialog.JSVDialog;
import jspecview.dialog.DialogManager;
import jspecview.dialog.DialogParams;
import jspecview.dialog.PlatformDialog;
import jspecview.source.JDXSpectrum;

class JsDialogManager extends DialogManager {

	@Override
	public PlatformDialog getDialog(JSVDialog jsvDialog,
			DialogParams params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDialogInput(Object parentComponent, String phrase,
			String title, int msgType, Object icon, Object[] objects,
			String defaultStr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getLocationOnScreen(Object component) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getOptionFromDialog(Object frame, String[] items, JSVPanel jsvp,
			String dialogName, String labelName) {
		// for export only
		return 0;
	}

	@Override
	public void showProperties(Object frame, JDXSpectrum spectrum) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void showText(Object frame, String title, String text) {
		/**
		 * 
		 * could be fancier
		 * 
		 * @j2sNative
		 * 
		 * alert(text);
		 */
		{}
	}
	
	@Override
	public void showMessageDialog(Object parentComponent, String msg,
			String title, int msgType) {
		/**
		 * @j2sNative
		 * 
		 * alert(msg);
		 */
		{
			
		}
	}

}

package jspecview.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import jspecview.android.R;

public class PreferencesManager {
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	private Activity activity;
		
	private static final String LAST_DIR = "pref_key_last_dir";
	
	public static final String START_DIR = "/sdcard";
		
	public PreferencesManager(Activity activity) {
		this.activity = activity;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		this.editor = prefs.edit();
	}
	
	public boolean isRememberLastDirectoryEnabled(){
		return prefs.getBoolean(activity.getString(R.string.pref_key_remember_last_dir), false);
	}
	
	public String getLastDirectory(){
		return prefs.getString(LAST_DIR, null);
	}
	
	public PreferencesManager setLastDirectory(String lastDir){		
		editor.putString(LAST_DIR, lastDir);
		editor.commit();
		return this;
	}
	
	public int getIntegralPlotColor(){
		String colorString = prefs.getString(activity.getString(R.string.pref_key_integ_plot_color), null);
		return Color.parseColor(colorString);
	}
}

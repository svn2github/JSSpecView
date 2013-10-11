package org.jmol.api;

public interface JSmolInterface {

	Object getApplet();

	boolean handleOldJvm10Event(int id, int x, int y, int modifiers, long time);

	void processTwoPointGesture(float[][][] touches);

	void setScreenDimension(int width, int height);

	void startHoverWatcher(boolean enable);

	void updateJS(int width, int height);

	int cacheFileByName(String fileName, boolean isAdd);
	
	void cachePut(String key, Object data);
	
	void openFileAsyncPDB(String fileName, boolean pdbCartoons);
	
}


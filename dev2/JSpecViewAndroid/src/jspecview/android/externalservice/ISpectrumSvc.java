package jspecview.android.externalservice;

import java.io.InputStream;

public interface ISpectrumSvc {
	public InputStream getSpectrum(String name) throws SpectrumSvcException;
	public void setProxy(String hostName, int port);
}

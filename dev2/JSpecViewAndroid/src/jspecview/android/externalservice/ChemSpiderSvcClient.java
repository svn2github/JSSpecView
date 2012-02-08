package jspecview.android.externalservice;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

public class ChemSpiderSvcClient implements ISpectrumSvc {
	
	private String url = "http://www.chemspider.com/";
	private String securityToken = "d8efe1b6-c538-4c89-bd4c-80fa21237c28";
	private String downloadUrl = "FilesHandler.ashx?type=blob&disp=1&tk=d8efe1b6-c538-4c89-bd4c-80fa21237c28&id=";
	private String xmlns = "http://www.chemspider.com/";
	private HttpHost proxy = null;
	
	
	public ChemSpiderSvcClient(){		
	}
	
	public int simpleSearch(String term) throws SpectrumSvcException{
		String url = this.url + "Search.asmx/SimpleSearch";
			
		int returnValue = Integer.MIN_VALUE;
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("query", term));
		params.add(new BasicNameValuePair("token", securityToken));
		
		try {
			
			InputStream response = HttpRequestHelper.postData(url, params, proxy);
			
			returnValue = parseSimpleSearchResponse(response);			
		}
		catch (SpectrumSvcException se){
			throw se;
		}
		catch (Exception e) {
			throw new SpectrumSvcException(e);
		}
		
		return returnValue;
	}
	
	protected class SimpleSearchResult {
		public int Result = Integer.MIN_VALUE;
	}
	
	protected int parseSimpleSearchResponse(InputStream xml) throws SpectrumSvcException{
		RootElement root = new RootElement(xmlns, "ArrayOfInt");
		Element intElement = root.getChild(xmlns, "int");
		final SimpleSearchResult value = new SimpleSearchResult();
		
		intElement.setEndTextElementListener(new EndTextElementListener() {			
			@Override
			public void end(String body) {
				value.Result = Integer.parseInt(body);				
			}
		});
		
		try {
            Xml.parse(xml, Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (Exception e) {
            throw new SpectrumSvcException("Error parsing response", e);
        }
        		
		return value.Result;
	}
	
	public List<ChemSpiderSpectrumInfo> getSpectraInfoArray(int chemSpiderId) throws SpectrumSvcException{
		String url = this.url + "Spectra.asmx/GetSpectraInfoArray";
		
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("CSIDs", String.valueOf(chemSpiderId)));
		params.add(new BasicNameValuePair("token", securityToken));
		
		List<ChemSpiderSpectrumInfo> returnValue = new ArrayList<ChemSpiderSpectrumInfo>();
		
		try {
			InputStream response = HttpRequestHelper.postData(url, params, proxy);
			returnValue = parseGetSpectraInfoArrayResponse(response);	
			
			// error string: "Unable to get records spectra."
			
		} catch (Exception e) {
			throw new SpectrumSvcException(e);
		}
		
		return returnValue;
	}
	
	
	protected List<ChemSpiderSpectrumInfo> parseGetSpectraInfoArrayResponse(InputStream xml) throws SpectrumSvcException{
		
		final List<ChemSpiderSpectrumInfo> specInfoList = new ArrayList<ChemSpiderSpectrumInfo>();
		final ChemSpiderSpectrumInfo currentSpecInfo = new ChemSpiderSpectrumInfo();
		
		RootElement root = new RootElement(xmlns, "ArrayOfCSSpectrumInfo");
		Element csSpectrumInfo = root.getChild(xmlns, "CSSpectrumInfo");
		
		csSpectrumInfo.setEndElementListener(new EndElementListener() {
			
			@Override
			public void end() {
				specInfoList.add(currentSpecInfo.copy());
			}
		});
		
		csSpectrumInfo.getChild(xmlns, "spc_id").setEndTextElementListener(new EndTextElementListener() {			
			@Override
			public void end(String body) {
				currentSpecInfo.setSpectrumId(Integer.parseInt(body));				
			}
		});
		
		csSpectrumInfo.getChild(xmlns, "csid").setEndTextElementListener(new EndTextElementListener() {			
			@Override
			public void end(String body) {
				currentSpecInfo.setChemSpiderId(Integer.parseInt(body));				
			}
		});
		
		csSpectrumInfo.getChild(xmlns, "file_name").setEndTextElementListener(new EndTextElementListener() {			
			@Override
			public void end(String body) {
				currentSpecInfo.setFileName(body);				
			}
		});
		
		csSpectrumInfo.getChild(xmlns, "comments").setEndTextElementListener(new EndTextElementListener() {			
			@Override
			public void end(String body) {
				currentSpecInfo.setComments(body);				
			}
		});
		
		csSpectrumInfo.getChild(xmlns, "original_url").setEndTextElementListener(new EndTextElementListener() {			
			@Override
			public void end(String body) {
				currentSpecInfo.setOriginalUrl(body);				
			}
		});
		
		csSpectrumInfo.getChild(xmlns, "submitted_date").setEndTextElementListener(new EndTextElementListener() {			
			@Override
			public void end(String body) {
				try {
					currentSpecInfo.setSubmittedDate(DateFormat.getInstance().parse(body));
				} catch (ParseException e) {
					
				}				
			}
		});
		
		try {
            Xml.parse(xml, Xml.Encoding.UTF_8, root.getContentHandler());
        } catch (Exception e) {
            throw new SpectrumSvcException("Error parsing response", e);
        }
		
		return specInfoList;
	}

	@Override
	public InputStream getSpectrum(String name) throws SpectrumSvcException {
		int chemSpiderId = simpleSearch(name);
		if(chemSpiderId == Integer.MIN_VALUE){
			throw new SpectrumSvcException("Spectrum ID not found");
		}
		List<ChemSpiderSpectrumInfo> specList = getSpectraInfoArray(chemSpiderId);
		if(specList.size() == 0){
			throw new SpectrumSvcException("Spectrum file not found");
		}
		
		int spectrumId = specList.get(0).getSpectrumId();
		
		InputStream is;
		
		try {
			is = HttpRequestHelper.getData(url + downloadUrl + String.valueOf(spectrumId), proxy);
		} catch (Exception e) {
			throw new SpectrumSvcException(e);
		}
		
		return is;
	}

	@Override
	public void setProxy(String hostName, int port) {
		proxy = new HttpHost(hostName, port);
	}
}


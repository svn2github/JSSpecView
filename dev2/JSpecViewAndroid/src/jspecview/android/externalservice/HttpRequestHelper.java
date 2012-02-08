package jspecview.android.externalservice;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpRequestHelper {
	
	public static InputStream postData(String url, List<NameValuePair> params, HttpHost proxy) 
		throws IllegalStateException, IOException {
	    // Create a new HttpClient and Post Header
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpPost httpPost = new HttpPost(url);
	    	        
    	// Add your data	        
	    httpPost.setEntity(new UrlEncodedFormEntity(params));

        // Set proxy
        if(proxy != null){
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
        
        // Execute HTTP Post Request
        HttpResponse response = httpClient.execute(httpPost);
        
        InputStream is = response.getEntity().getContent();
        
        return is;
        
	} 
	
	public static InputStream getData(String url, HttpHost proxy) 
		throws IllegalStateException, IOException {
	    // Create a new HttpClient and Post Header
	    HttpClient httpClient = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(url);
	    	        
	    // Set proxy
	    if(proxy != null){
			httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
	    
	    // Execute HTTP Post Request
	    HttpResponse response = httpClient.execute(httpGet);
	    
	    InputStream is = response.getEntity().getContent();
	    
	    return is;
	    
	} 
}

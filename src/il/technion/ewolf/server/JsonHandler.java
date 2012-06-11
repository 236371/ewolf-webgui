package il.technion.ewolf.server;

import il.technion.ewolf.server.fetchers.JsonDataFetcher;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonHandler implements HttpRequestHandler {
	
	Map<String,JsonDataFetcher> fetchers = new HashMap<String,JsonDataFetcher>();
	
	public class jSonData {
		
		public jSonData(String itsKey, Object itsJSonData) {
			key = itsKey;
			data = itsJSonData;
		}
		
		public String key;
		public Object data;
	}
	
	public JsonHandler addFetcher(String key, JsonDataFetcher fetcher) {
		fetchers.put(key, fetcher);
		return this;
	}
	
	@Override
	public void handle(HttpRequest req, HttpResponse res, HttpContext ctx)
			throws HttpException, IOException {
		String uri = req.getRequestLine().getUri();
		System.out.println("\t[JsonHandler] requesting: " + uri);
		
		String callBack = null;
		List<JsonHandler.jSonData> lst = new ArrayList<JsonHandler.jSonData>();
		
		try {
			
			List<NameValuePair> parameters = 
					URLEncodedUtils.parse(new URI(uri),HTTP.UTF_8);
			
			for (NameValuePair nameValuePair : parameters) {
				String key = nameValuePair.getName();
				
				JsonDataFetcher fetcher = fetchers.get(nameValuePair.getName());
				if(fetcher != null) {
					String[] fetchParameters = nameValuePair.getValue().split(",");
					Object o = null;

					try {
						o = fetcher.fetchData(fetchParameters);
					} catch (ProfileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if(o != null) {
						lst.add(new jSonData(key, o));
					}
				}
				
				if(key.equals("callBack")) {
					callBack = new String(nameValuePair.getValue());
				}
			}			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		Gson gson = new GsonBuilder()
        	.serializeNulls()
        	.create();
		
		String s = gson.toJson(lst, lst.getClass());
		if(callBack != null) {
			s = callBack + "(" + s  + ")";
		}

		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		res.addHeader(HTTP.CONTENT_TYPE, "application/json");
		
		res.setEntity(new StringEntity(s,HTTP.UTF_8));
	}

}

package il.technion.ewolf.server;

import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.server.handlers.JsonDataHandler;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
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
	
	private Map<String,JsonDataHandler> handlers = new HashMap<String,JsonDataHandler>();
	
	public class jSonData {
		
		public jSonData(String itsKey, Object itsJSonData) {
			key = itsKey;
			data = itsJSonData;
		}
		
		public String key;
		public Object data;
	}
	
	public JsonHandler addHandler(String key, JsonDataHandler fetcher) {
		handlers.put(key, fetcher);
		return this;
	}
	
	@Override
	public void handle(HttpRequest req, HttpResponse res, HttpContext ctx)
			throws HttpException {
		String uri = req.getRequestLine().getUri();
		System.out.println("\t[JsonHandler] requesting: " + uri);
		
		String callBack = null;
		List<JsonHandler.jSonData> lst = new ArrayList<JsonHandler.jSonData>();
		
		try {
			
			List<NameValuePair> parameters = 
					URLEncodedUtils.parse(new URI(uri),HTTP.UTF_8);
			
			for (NameValuePair nameValuePair : parameters) {
				String key = nameValuePair.getName();
				
				JsonDataHandler fetcher = handlers.get(key);
				if(fetcher != null) {
					String[] handlerParameters = nameValuePair.getValue().split(",");
					Object o = null;

					try {
						o = fetcher.handleData(handlerParameters);
					} catch (ProfileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (WallNotFound e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (FileNotFoundException e) {
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
        	.disableHtmlEscaping()
        	.create();
		
		String s = gson.toJson(lst, lst.getClass());
		if(callBack != null) {
			s = callBack + "(" + s  + ")";
		}

		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		res.addHeader(HTTP.CONTENT_TYPE, "application/json");
		
		try {
			res.setEntity(new StringEntity(s, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

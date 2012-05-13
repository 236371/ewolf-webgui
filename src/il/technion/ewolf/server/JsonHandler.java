package il.technion.ewolf.server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.google.gson.Gson;

public class JsonHandler implements HttpRequestHandler {
	
	public class jSonData {
		
		public jSonData(String t, String k) {
			title = t;
			key = k;
		}
		
		public String title;
		public String key;
	}
	
	@Override
	public void handle(HttpRequest req, HttpResponse res, HttpContext ctx)
			throws HttpException, IOException {
		String uri = req.getRequestLine().getUri();
		System.out.println("\t[JsonHandler] requesting: " + uri);
		
		String callBack = new String();
		try {
			List<NameValuePair> parameters = 
					URLEncodedUtils.parse(new URI(uri),"HTTP.UTF_8");
			
			for (NameValuePair nameValuePair : parameters) {
//				System.out.println(nameValuePair.getName() +
//						": " + nameValuePair.getValue());
				if(nameValuePair.getName().equals("callBack")) {
					callBack = new String(nameValuePair.getValue());
				}
			}			
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		Gson gson = new Gson();
		
		List<JsonHandler.jSonData> lst = new ArrayList<JsonHandler.jSonData>();
		lst.add(new jSonData("Show cats","cat"));
		lst.add(new jSonData("Show people","people"));
		lst.add(new jSonData("Show wolf","wolf"));
		lst.add(new jSonData("Show models","model"));
		
		String s = callBack + "(" + gson.toJson(lst, lst.getClass()) + ")";

		res.addHeader("Server", "e-WolfNode");
		res.addHeader("Content-Type", "application/json");
		
		res.setEntity(new StringEntity(s));
	}

}

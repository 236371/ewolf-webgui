package il.technion.ewolf.server;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.lang.Long;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.InputStreamEntity;

import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class FaviconHandler implements HttpRequestHandler {
	
	@Override
	public void handle(HttpRequest req, HttpResponse res, HttpContext ctx)
			throws HttpException, IOException {
		String resName = req.getRequestLine().getUri();
		System.out.println(resName);
		
		resName = resName.substring(1);
		
		URL url = getClass().getResource(resName);
		Date modDataVar =  new Date(url.openConnection().getLastModified());
		String modDate = new Long(modDataVar.getTime()).toString();
		
		Header[] h = req.getHeaders("IF-MODIFIED-SINCE");
		if(h.length != 0) {
			for (Header header : h) {
				if(header.getValue().equals(modDate)) {
					res.setHeader("304 Not Modified", "");
					System.out.println("Not Modified");
					return;
				}
			}			
		}
				
		res.addHeader("Connection", "close");
		res.addHeader("Server", "e-WolfNode");
		res.addHeader("Content-Type", "image/gif");
		res.addHeader("Last-Modified", modDate);
		
	    res.setEntity(new InputStreamEntity(url.openStream(), -1));
	}

}

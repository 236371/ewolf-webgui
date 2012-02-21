package il.technion.ewolf.chunkeeper.handlers;

import il.technion.ewolf.http.HttpConnector;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * Base class for all chunkeeper handlers
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public abstract class AbstractHandler implements HttpRequestHandler {
	
	
	protected abstract String getPath();
	
	/**
	 * Parse an http request for its query part
	 * @param req the http request
	 * @return parsed http query
	 * @throws HttpException
	 */
	protected Map<String, List<String>> parseQuery(HttpRequest req) throws HttpException {
		Map<String, List<String>> params = new HashMap<String, List<String>>();
		
		URI uri;
		try {
			uri = new URI(req.getRequestLine().getUri());
		} catch (URISyntaxException e1) {
			throw new HttpException("uri syntax error: "+req.getRequestLine().getUri());
		}
		try {
			if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
				for (String pair : uri.getQuery().split("&")) {
					String k = URLDecoder.decode(pair.split("=")[0], "UTF-8");
					String v = URLDecoder.decode(pair.split("=")[1], "UTF-8");
					
					List<String> l = params.get(k);
					if (l == null) {
						l = new ArrayList<String>();
						params.put(k, l);
					}
					l.add(v);
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return params;
	}
	
	/**
	 * Register this handler
	 * @param conn the http connector used by the chunkeeper to transfer messages and data
	 */
	public void register(HttpConnector conn) {
		conn.register(getPath(), this);
	}
}

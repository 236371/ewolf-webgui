package il.technion.ewolf.server;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

@Deprecated
public class HttpStringExtractor {
	
	public static String fromURIAfterLastSlash(HttpRequest req) {
		String reqURI = req.getRequestLine().getUri();
		String[] splitedURI = reqURI.split("/");
		String strUid = splitedURI[splitedURI.length-1];
		return strUid;
	}
	
	public static String fromURIAfterPrefix(HttpRequest req, String prefix) {
		String reqURI = req.getRequestLine().getUri();
		String strUid = reqURI.substring(prefix.length()-1);
		return strUid;
	}
	
	public static String fromBodyAfterFirstEqualsSign(HttpRequest req) throws ParseException, IOException {
		String dataSet = EntityUtils.toString(((HttpEntityEnclosingRequest)req).getEntity());
		String value = dataSet.substring(dataSet.indexOf("=") + 1);
		return value;
	}

}

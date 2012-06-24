package il.technion.ewolf.server.handlers;

import il.technion.ewolf.server.ServerResources;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class JarResourceHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		String reqUri = request.getRequestLine().getUri();
		System.out.println("\t[JarResourceHandler] requesting: " + reqUri);
		//TODO move adding general headers to response intercepter
		response.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		if(reqUri.equals("/")) {
			reqUri = "/home.html";
		}
		
		String path = "/www" + reqUri; // TODO prevent directory traversing
		InputStream is = getClass().getResourceAsStream(path);

		if (is==null) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			return;
		}
		
		ByteArrayEntity bae = new ByteArrayEntity(IOUtils.toByteArray(is));
		response.addHeader(HTTP.CONTENT_TYPE, ServerResources.getFileTypeMap().getContentType(path));
		response.setEntity(bae);
	}
}

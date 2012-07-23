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
	private static final String PAGE_404 = "/404.html";

	@Override
	public void handle(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		String reqUri = request.getRequestLine().getUri();
		System.out.println("\t[JarResourceHandler] requesting: " + reqUri);
		//TODO move adding general headers to response intercepter
		response.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		if (reqUri.contains("..")) {
			response.setStatusCode(HttpStatus.SC_FORBIDDEN);
			return;
		}

		if(reqUri.equals("/")) {
			reqUri = "/home.html";
		}
		
		String path = "/www" + reqUri;
		InputStream is = getClass().getResourceAsStream(path);

		if (is==null) {
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);
			path = "/www" + PAGE_404;
			is = getClass().getResourceAsStream(path);
			if (is == null) return;
		}
		
		setResponseEntity(response, is);
		response.addHeader(HTTP.CONTENT_TYPE, ServerResources.getFileTypeMap().getContentType(path));
	}

	private void setResponseEntity(HttpResponse response, InputStream is) throws IOException {
		ByteArrayEntity bae = new ByteArrayEntity(IOUtils.toByteArray(is));
		response.setEntity(bae);
	}
}

package il.technion.ewolf.server.handlers;

import il.technion.ewolf.server.EwolfServer;
import il.technion.ewolf.server.ServerResources;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class JarResourceHandler implements HttpRequestHandler {
	private static final String PAGE_404 = "/404.html";
	private EwolfServer ewolfServer;

	public JarResourceHandler(EwolfServer ewolfServer) {
		this.ewolfServer = ewolfServer;
	}

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws IOException {
		String reqUri = req.getRequestLine().getUri();
		System.out.println("\t[JarResourceHandler] requesting: " + reqUri);
		//TODO move adding general headers to response intercepter
		res.addHeader(HttpHeaders.SERVER, "e-WolfNode");

		try {
			String dateString = req.getLastHeader(HttpHeaders.IF_MODIFIED_SINCE).getValue();
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			if (dateString != null) {
				Date d = format.parse(dateString);
				if (d.after(ewolfServer.startTime())) {
					res.setStatusCode(HttpStatus.SC_NOT_MODIFIED);
					return;
				}
			}
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (reqUri.contains("..")) {
			res.setStatusCode(HttpStatus.SC_FORBIDDEN);
			return;
		}

		if(reqUri.equals("/")) {
			reqUri = "/home.html";
		}

		String path = "/www" + reqUri;
		InputStream is = getResourceAsStream(path);
		if (is == null) {
			res.setStatusCode(HttpStatus.SC_NOT_FOUND);
			path = "/www" + PAGE_404;
			is = getResourceAsStream(path);
			if (is == null) return;
		}

		setResponseEntity(res, is, path);
	}

	InputStream getResourceAsStream(String path) {
		return getClass().getResourceAsStream(path);
	}

	void setResponseEntity(HttpResponse response, InputStream is, String path) throws IOException {
		ByteArrayEntity bae = new ByteArrayEntity(IOUtils.toByteArray(is));
		response.setEntity(bae);
		response.addHeader(HttpHeaders.CONTENT_TYPE, ServerResources.getFileTypeMap().getContentType(path));
	}
}

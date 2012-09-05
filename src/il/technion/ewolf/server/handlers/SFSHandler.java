package il.technion.ewolf.server.handlers;

import il.technion.ewolf.server.ServerResources;
import il.technion.ewolf.server.ewolfHandlers.DownloadFileFromSFS;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class SFSHandler implements HttpRequestHandler {
	private DownloadFileFromSFS handler;

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) {
		//TODO move adding server header to response intercepter
		res.addHeader(HttpHeaders.SERVER, "e-WolfNode");

		if (req.containsHeader(HttpHeaders.IF_MODIFIED_SINCE)) {
			res.setStatusCode(HttpStatus.SC_NOT_MODIFIED);
			return;
		}

		String uri = req.getRequestLine().getUri();
		try {
			List<NameValuePair> parameters = 
					URLEncodedUtils.parse(new URI(uri).getRawQuery(), Consts.UTF_8);
			String fileName = null;
			String userID = null;
			for (NameValuePair p : parameters) {
				String name = p.getName();

				if (name.equals("fileName")) {
					fileName = p.getValue();
				}
				if (name.equals("userID")) {
					userID = p.getValue();
				}
			}
			if (userID == null || fileName == null) {
				setResponse(res, HttpStatus.SC_NOT_FOUND);
				return;
			}
			Serializable fileData = handler.handleData(userID, fileName);
			String mimeType = ServerResources.getFileTypeMap().getContentType(fileName);
			res.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
			res.setHeader( "Content-Disposition", "attachment; filename=" + fileName );
			res.setEntity(new ByteArrayEntity((byte[]) fileData));
		} catch (Exception e) {
			e.printStackTrace();
			setResponse(res, HttpStatus.SC_NOT_FOUND);
			return;
		}

	}

	private void setResponse(HttpResponse res, int resStatus) {
		res.setStatusCode(resStatus);
		if (resStatus == HttpStatus.SC_NOT_FOUND) {
			try {
				String path = "/www/404.html";
				JarResourceHandler handler = new JarResourceHandler();
				InputStream is = handler.getResourceAsStream(path);
				if (is == null) return;
				handler.setResponseEntity(res, is, path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void addHandler(DownloadFileFromSFS handler) {
		this.handler = handler;
	}

}

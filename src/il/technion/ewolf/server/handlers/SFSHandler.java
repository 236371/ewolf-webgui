package il.technion.ewolf.server.handlers;

import il.technion.ewolf.server.ServerResources;
import il.technion.ewolf.server.ewolfHandlers.DownloadFileFromSFS;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class SFSHandler implements HttpRequestHandler {
	private DownloadFileFromSFS handler;

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) {
		//TODO move adding server header to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
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
				//TODO reply bad request
				return;
			}
			String strEntity = handler.handleData(userID, fileName);
			String mimeType = ServerResources.getFileTypeMap().getContentType(fileName);
			res.setEntity(new StringEntity(strEntity, ContentType.create(mimeType, Consts.UTF_8)));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void addHandler(DownloadFileFromSFS handler) {
		this.handler = handler;
	}

}

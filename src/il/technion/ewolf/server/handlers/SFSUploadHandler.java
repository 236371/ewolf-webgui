package il.technion.ewolf.server.handlers;

import static il.technion.ewolf.server.jsonDataHandlers.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.jsonDataHandlers.EWolfResponse.RES_INTERNAL_SERVER_ERROR;
import static il.technion.ewolf.server.jsonDataHandlers.EWolfResponse.RES_SUCCESS;
import il.technion.ewolf.server.ewolfHandlers.UploadFileToSFS;
import il.technion.ewolf.server.jsonDataHandlers.EWolfResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SFSUploadHandler implements HttpRequestHandler {
	private UploadFileToSFS handler;

	class SFSUploadHandlerResponse extends EWolfResponse {
		String path;
		public SFSUploadHandlerResponse(String result, String path) {
			super(result);
			this.path = path;
		}
	}

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) {
		//TODO move adding server header to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");
		
		String uri = req.getRequestLine().getUri();

		String wolfpackName = null;
		String ext = null;

		try {
			List<NameValuePair> parameters = 
					URLEncodedUtils.parse(new URI(uri).getRawQuery(),Consts.UTF_8);
			for (NameValuePair v : parameters) {
				String name = v.getName();

				if (name.equals("fileName")) {
					String fileName = v.getValue();
					String[] splitedFileName = fileName.split("\\.");
					ext = splitedFileName[splitedFileName.length-1];
				}
				if (name.equals("wolfpackName")) {
					wolfpackName = v.getValue();
				}
			}
			if (ext == null || wolfpackName == null) {
				setResponse(res, null, RES_BAD_REQUEST);
				return;
			}

			String fileData = EntityUtils.toString(((HttpEntityEnclosingRequest)req).getEntity(), Consts.UTF_8);
			
			Object resObj = handler.handleData(wolfpackName, ext, fileData);
			setResponse(res, resObj, RES_SUCCESS);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			setResponse(res, null, RES_BAD_REQUEST);
			return;
		} catch (ParseException e) {
			e.printStackTrace();
			setResponse(res, null, RES_INTERNAL_SERVER_ERROR);
			return;
		} catch (IOException e) {
			e.printStackTrace();
			setResponse(res, null, RES_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	private void setResponse(HttpResponse res, Object resObj, String result) {
		Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
		String json = gson.toJson(resObj);
		res.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
	}
	
	public void addHandler(UploadFileToSFS handler) {
		this.handler = handler;
	}
}

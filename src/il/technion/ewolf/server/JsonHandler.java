package il.technion.ewolf.server;

import il.technion.ewolf.server.exceptions.BadRequestException;
import il.technion.ewolf.server.exceptions.HandlerException;
import il.technion.ewolf.server.exceptions.InternalEwolfErrorException;
import il.technion.ewolf.server.exceptions.NotFoundException;
import il.technion.ewolf.server.handlers.JsonDataHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonHandler implements HttpRequestHandler {
	private Map<String,JsonDataHandler> handlers = new HashMap<String,JsonDataHandler>();

	public JsonHandler addHandler(String key, JsonDataHandler handler) {
		handlers.put(key, handler);
		return this;
	}

	@Override
	public void handle(HttpRequest req, HttpResponse res,
			HttpContext context) throws HttpException, IOException {
		//TODO move adding server header to response intercepter
		res.addHeader(HTTP.SERVER_HEADER, "e-WolfNode");

		String jsonReqAsString = EntityUtils.toString(((HttpEntityEnclosingRequest)req).getEntity());
		JsonParser parser = new JsonParser();
        JsonObject jsonReq = parser.parse(jsonReqAsString).getAsJsonObject();
        
        JsonObject jsonRes = new JsonObject();
        Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
        
        Set<Entry<String, JsonElement>> jsonReqAsSet = jsonReq.entrySet();
        for (Entry<String, JsonElement> obj : jsonReqAsSet) {
        	String key = obj.getKey();
        	JsonDataHandler handler = handlers.get(key);
        	if (handler != null) {
        		try {
        			Object handlerRes = handler.handleData(obj.getValue());
        			jsonRes.add(key, gson.toJsonTree(handlerRes));
        		} catch (NotFoundException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		} catch (InternalEwolfErrorException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		} catch (BadRequestException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		} catch (HandlerException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
        	} else {
        		System.out.println("No handlers are registered to handle request " +
        				"with keyword " + key);
        		//TODO send response with 400 Bad Request status
        	}     	
		}
        
        String json = gson.toJson(jsonRes);
        res.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
	}
}

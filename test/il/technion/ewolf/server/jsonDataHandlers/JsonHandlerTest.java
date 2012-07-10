package il.technion.ewolf.server.jsonDataHandlers;

import il.technion.ewolf.server.jsonDataHandlers.JsonDataHandler;

import com.google.gson.JsonElement;

public class JsonHandlerTest implements JsonDataHandler{

	@Override
	public Object handleData(JsonElement jsonElement) {
		return jsonElement;
	}

}

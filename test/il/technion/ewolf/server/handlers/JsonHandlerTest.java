package il.technion.ewolf.server.handlers;

import com.google.gson.JsonElement;

public class JsonHandlerTest implements JsonDataHandler{

	@Override
	public JsonElement handleData(JsonElement jsonElement) {
		return jsonElement;
	}

}

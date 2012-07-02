package il.technion.ewolf.server.handlers;

import com.google.gson.JsonElement;

public interface JsonDataHandler {
	public JsonElement handleData(JsonElement jsonElement);
}

package il.technion.ewolf.server.jsonDataHandlers;

import com.google.gson.JsonElement;

public interface JsonDataHandler {
	public Object handleData(JsonElement jsonElement);
}

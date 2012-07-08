package il.technion.ewolf.server.handlers;

import com.google.gson.JsonElement;

public interface JsonDataHandler {
	public Object handleData(JsonElement jsonElement);
}

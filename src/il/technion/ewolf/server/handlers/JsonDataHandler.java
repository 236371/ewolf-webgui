package il.technion.ewolf.server.handlers;

import il.technion.ewolf.server.exceptions.HandlerException;

import com.google.gson.JsonElement;

public interface JsonDataHandler {
	public Object handleData(JsonElement jsonElement) throws HandlerException;
}

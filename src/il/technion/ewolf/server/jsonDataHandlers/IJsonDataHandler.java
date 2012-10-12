package il.technion.ewolf.server.jsonDataHandlers;

import il.technion.ewolf.server.EWolfResponse;

import com.google.gson.JsonElement;

public interface IJsonDataHandler {
	public EWolfResponse handleData(JsonElement jsonElement);
}

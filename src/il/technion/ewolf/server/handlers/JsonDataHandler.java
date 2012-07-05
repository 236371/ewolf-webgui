package il.technion.ewolf.server.handlers;

import il.technion.ewolf.server.exceptions.HandlerException;

import com.google.gson.JsonElement;

public interface JsonDataHandler {
	static final String RES_SUCCESS = "success";
	static final String RES_NOT_FOUND = "not found";
	static final String RES_BAD_REQUEST = "bad request";
	static final String RES_INTERNAL_SERVER_ERROR = "internal server error";
	static final String RES_GENERIC_ERROR = "error";

	public Object handleData(JsonElement jsonElement) throws HandlerException;
}

package il.technion.ewolf.server.jsonDataHandlers;

import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_INTERNAL_SERVER_ERROR;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.server.ServerResources;
import il.technion.ewolf.server.ServerResources.EwolfConfigurations;

import org.apache.commons.configuration.ConfigurationException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class LoginHandler implements IJsonDataHandler {
	private String configFile;

	public LoginHandler(String configFile) {
		this.configFile = configFile;
	}

	private static class JsonReqLoginParams {
		String username;
		String password;
	}

	static class LoginResponse extends EWolfResponse {
		public LoginResponse(String result, String errorMessage) {
			super(result, errorMessage);
		}

		public LoginResponse(String result) {
			super(result);
		}

		public LoginResponse() {
		}
	}

	@Override
	public EWolfResponse handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqLoginParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqLoginParams.class);
		} catch (Exception e) {
			e.printStackTrace();
			return new LoginResponse(RES_BAD_REQUEST);
		}

		EwolfConfigurations configurations;
		try {
			configurations = ServerResources.getConfigurations(configFile);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			return new LoginResponse(RES_INTERNAL_SERVER_ERROR);
		}

		if (jsonReqParams.username == null || jsonReqParams.password == null
				|| !configurations.username.equals(jsonReqParams.username)
				|| !configurations.password.equals(jsonReqParams.password)) {
			return new LoginResponse(RES_BAD_REQUEST, "Username or password is incorrect.");
		}

		return new LoginResponse();
	}

}

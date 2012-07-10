package il.technion.ewolf.server.jsonDataHandlers;

import il.technion.ewolf.ewolf.WolfPackLeader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import static il.technion.ewolf.server.jsonDataHandlers.EWolfResponse.*;

public class CreateWolfpackHandler implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;
	
	@Inject
	public CreateWolfpackHandler(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
	}
	
	private class JsonReqCreateWolfpackParams {
		String wolfpackName;
	}

	class CreateWolfpackResponse extends EWolfResponse {
		public CreateWolfpackResponse(String result) {
			super(result);
		}
		public CreateWolfpackResponse() {
		}
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqCreateWolfpackParams class
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqCreateWolfpackParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqCreateWolfpackParams.class);
		} catch (Exception e) {
			return new CreateWolfpackResponse(RES_BAD_REQUEST);
		}

		if (jsonReqParams.wolfpackName == null) {
			return new CreateWolfpackResponse(RES_BAD_REQUEST + ": must specify wolfpack name.");
		}
		//TODO do we want to get feedback about this?
		if (socialGroupsManager.findSocialGroup(jsonReqParams.wolfpackName) != null) {
			return new CreateWolfpackResponse(RES_BAD_REQUEST + ": wolfpack already exists");
		}
		try {
			socialGroupsManager.findOrCreateSocialGroup(jsonReqParams.wolfpackName);
		} catch (Exception e) {
			return new CreateWolfpackResponse(RES_INTERNAL_SERVER_ERROR);
		}
		return new CreateWolfpackResponse();
	}

}
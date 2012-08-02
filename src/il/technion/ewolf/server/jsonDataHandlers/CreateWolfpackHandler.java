package il.technion.ewolf.server.jsonDataHandlers;

import java.util.ArrayList;
import java.util.List;

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
	
	private static class JsonReqCreateWolfpackParams {
		List<String> wolfpackNames;
	}

	static class CreateWolfpackResponse extends EWolfResponse {
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

		if (jsonReqParams.wolfpackNames == null || jsonReqParams.wolfpackNames.isEmpty()) {
			return new CreateWolfpackResponse(RES_BAD_REQUEST + ": must specify wolfpack name/s.");
		}

		List<CreateWolfpackResponse> wolfpacksResult = new ArrayList<CreateWolfpackResponse>();
		for (String wolfpackName : jsonReqParams.wolfpackNames) {
			//TODO do we want to get feedback about this?
			if (socialGroupsManager.findSocialGroup(wolfpackName) != null) {
				wolfpacksResult.add(new CreateWolfpackResponse(RES_BAD_REQUEST + ": wolfpack already exists"));
				continue;
			}
			try {
				socialGroupsManager.findOrCreateSocialGroup(wolfpackName);
			} catch (Exception e) {
				wolfpacksResult.add(new CreateWolfpackResponse(RES_INTERNAL_SERVER_ERROR));
				continue;
			}
			wolfpacksResult.add(new CreateWolfpackResponse());
		}
		for (CreateWolfpackResponse res : wolfpacksResult) {
			if (res.result() != RES_SUCCESS) {
				return wolfpacksResult;
			}
		}
		return new CreateWolfpackResponse();
	}

}

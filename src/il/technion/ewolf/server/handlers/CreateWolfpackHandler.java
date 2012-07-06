package il.technion.ewolf.server.handlers;

import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.server.exceptions.InternalEwolfErrorException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class CreateWolfpackHandler implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;
	
	@Inject
	public CreateWolfpackHandler(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
	}
	
	private class JsonReqCreateWolfpackParams {
		String wolfpackName;
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqCreateWolfpackParams class
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqCreateWolfpackParams jsonReqParams =
				gson.fromJson(jsonReq, JsonReqCreateWolfpackParams.class);

		if (jsonReqParams.wolfpackName == null) {
			return new EWolfResponse("Must specify wolfpack name.");
		}
		if (socialGroupsManager.findSocialGroup(jsonReqParams.wolfpackName) != null) {
			return new EWolfResponse("wolfpack already exists");
		}
		try {
			socialGroupsManager.findOrCreateSocialGroup(jsonReqParams.wolfpackName);
		} catch (Exception e) {
			return new InternalEwolfErrorException(e);
		}
		//FIXME
		return new EWolfResponse();
	}

}

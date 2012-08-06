package il.technion.ewolf.server.jsonDataHandlers;

import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import static il.technion.ewolf.server.EWolfResponse.*;

public class AddWolfpackMemberHandler implements JsonDataHandler {
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	private final UserIDFactory userIDFactory;


	@Inject
	public AddWolfpackMemberHandler(SocialFS socialFS, WolfPackLeader socialGroupsManager, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
		this.userIDFactory = userIDFactory;
	}

	private static class JsonReqAddWolfpackMemberParams {
		String wolfpackName;
		String userID;
	}

	static class AddWolfpackMemberResponse extends EWolfResponse {
		public AddWolfpackMemberResponse(String result) {
			super(result);
		}
		public AddWolfpackMemberResponse() {
		}
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqAddWolfpackMemberParams class
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqAddWolfpackMemberParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqAddWolfpackMemberParams.class);
		} catch (Exception e) {
			e.printStackTrace();
			return new AddWolfpackMemberResponse(RES_BAD_REQUEST);
		}

		if (jsonReqParams.wolfpackName == null || jsonReqParams.userID==null) {
			return new AddWolfpackMemberResponse(RES_BAD_REQUEST + 
					": must specify both wolfpack name and user ID.");
		}
		
		Profile profile;
		try {
			UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
			profile = socialFS.findProfile(uid);
		} catch (ProfileNotFoundException e) {
			e.printStackTrace();
			return new AddWolfpackMemberResponse(RES_NOT_FOUND + ": user with given ID wasn't found.");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return new AddWolfpackMemberResponse(RES_BAD_REQUEST + ": illegal user ID.");
		}

		WolfPack socialGroup = socialGroupsManager.findSocialGroup(jsonReqParams.wolfpackName);
		if (socialGroup == null) {
			return new AddWolfpackMemberResponse(RES_NOT_FOUND + ": given wolfpack wasn't found.");
		}
		try {
			socialGroup.addMember(profile);
			//TODO check if allowed to add member several times
			socialGroupsManager.findSocialGroup("wall-readers").addMember(profile);
		} catch (GroupNotFoundException e) {
			e.printStackTrace();
			return new AddWolfpackMemberResponse(RES_INTERNAL_SERVER_ERROR);
		}
		
		return new AddWolfpackMemberResponse();
	}

}

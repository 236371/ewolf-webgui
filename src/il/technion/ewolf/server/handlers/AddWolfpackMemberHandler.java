package il.technion.ewolf.server.handlers;

import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;


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

	private class JsonReqAddWolfpackMemberParams {
		String wolfpackName;
		String userID;
	}

	//response error messages
	private static final String PROFILE_NOT_FOUND_MESSAGE = "user not found";
	private static final String INTERNAL_ERROR_MESSAGE = "internal error";
	private static final String WOLFPACK_NOT_FOUND_MESSAGE = "wolfpack not found";

	/**
	 * @param	jsonReq	serialized object of JsonReqAddWolfpackMemberParams class
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqAddWolfpackMemberParams jsonReqParams =
				gson.fromJson(jsonReq, JsonReqAddWolfpackMemberParams.class);
		if (jsonReqParams.wolfpackName == null || jsonReqParams.userID==null) {
			return "Must specify both wolfpack name and user ID.";
		}
		
		UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
		Profile profile;
		try {
			profile = socialFS.findProfile(uid);
		} catch (ProfileNotFoundException e) {
			e.printStackTrace();
			return PROFILE_NOT_FOUND_MESSAGE;
		}

		WolfPack socialGroup = socialGroupsManager.findSocialGroup(jsonReqParams.wolfpackName);
		if (socialGroup == null) {
			return WOLFPACK_NOT_FOUND_MESSAGE;
		}
		try {
			socialGroup.addMember(profile);
		} catch (GroupNotFoundException e) {
			e.printStackTrace();
			return INTERNAL_ERROR_MESSAGE;
		}
		
		return "success";
	}

}

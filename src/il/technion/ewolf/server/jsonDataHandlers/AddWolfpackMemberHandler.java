package il.technion.ewolf.server.jsonDataHandlers;

import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_GENERIC_ERROR;
import static il.technion.ewolf.server.EWolfResponse.RES_INTERNAL_SERVER_ERROR;
import static il.technion.ewolf.server.EWolfResponse.RES_NOT_FOUND;
import static il.technion.ewolf.server.EWolfResponse.RES_SUCCESS;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.util.ArrayList;
import java.util.List;

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

	private static class JsonReqAddWolfpackMemberParams {
		List<String> wolfpackNames;
		List<String> userIDs;
	}

	static class AddWolfpackMemberResponse extends EWolfResponse {
		List<EWolfResponse> wolfpacksResult;
		List<EWolfResponse> usersResult;
		public AddWolfpackMemberResponse(String result) {
			super(result);
		}

		public AddWolfpackMemberResponse(String result, String errorMessage) {
			super(result, errorMessage);
		}

		public AddWolfpackMemberResponse(String result, List<EWolfResponse> wolfpacksResult,
				List<EWolfResponse> usersResult) {
			super(result);
			this.usersResult = usersResult;
			this.wolfpacksResult = wolfpacksResult;
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

		if (jsonReqParams.wolfpackNames == null || jsonReqParams.wolfpackNames.isEmpty() || 
					jsonReqParams.userIDs==null || jsonReqParams.userIDs.isEmpty()) {
			return new AddWolfpackMemberResponse(RES_BAD_REQUEST, 
					"Must specify both wolfpack name and user ID.");
		}

		List<EWolfResponse> wolfpacksResult = new ArrayList<EWolfResponse>();
		List<EWolfResponse> usersResult = new ArrayList<EWolfResponse>();

		List<Profile> profiles = new ArrayList<Profile>();
		for (String userID : jsonReqParams.userIDs) {
			try {
				UserID uid = userIDFactory.getFromBase64(userID);
				profiles.add(socialFS.findProfile(uid));
				usersResult.add(new EWolfResponse());
			} catch (ProfileNotFoundException e) {
				e.printStackTrace();
				usersResult.add(new EWolfResponse(RES_NOT_FOUND,
						"User with the given ID wasn't found."));
				continue;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				usersResult.add(new EWolfResponse(RES_NOT_FOUND, "Illegal user ID."));
				continue;
			}
		}

		for (String wolfpackName : jsonReqParams.wolfpackNames) {
			WolfPack socialGroup = socialGroupsManager.findSocialGroup(wolfpackName);
			if (socialGroup == null) {
				wolfpacksResult.add(new EWolfResponse(RES_NOT_FOUND,
						"Given wolfpack wasn't found."));
				continue;
			}
			try {
				for (Profile profile : profiles) {
					socialGroup.addMember(profile);
					//TODO check if allowed to add member several times
					socialGroupsManager.findSocialGroup("wall-readers").addMember(profile);
				}
				wolfpacksResult.add(new EWolfResponse());
			} catch (GroupNotFoundException e) {
				e.printStackTrace();
				wolfpacksResult.add(new EWolfResponse(RES_INTERNAL_SERVER_ERROR));
			}
		}

		for (EWolfResponse res : wolfpacksResult) {
			if (res.getResult() != RES_SUCCESS) {
				return new AddWolfpackMemberResponse(RES_GENERIC_ERROR, wolfpacksResult, usersResult);
			}
		}
		for (EWolfResponse res : usersResult) {
			if (res.getResult() != RES_SUCCESS) {
				return new AddWolfpackMemberResponse(RES_GENERIC_ERROR, wolfpacksResult, usersResult);
			}
		}
		return new AddWolfpackMemberResponse(RES_SUCCESS);
	}

}

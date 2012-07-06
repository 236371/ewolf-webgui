package il.technion.ewolf.server.handlers;


import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class WolfpacksFetcher implements JsonDataHandler {
	private final SocialFS socialFS;
	private final WolfPackLeader socialGroupsManager;
	private final UserIDFactory userIDFactory;

	@Inject
	public WolfpacksFetcher(SocialFS socialFS, WolfPackLeader socialGroupsManager, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.socialGroupsManager = socialGroupsManager;
		this.userIDFactory = userIDFactory;
	}

	private class JsonReqWolfpacksParams {
//		If userID field wasn't sent with the request then the response
//			list of wolfpack names will be for "logged in" user
		String userID;
	}

	@SuppressWarnings("unused")
	class WolfpacksResponse extends EWolfResponse {
		private List<String> wolfpacksList;
		public WolfpacksResponse(List<String> lst, String result) {
			super(result);
			this.wolfpacksList = lst;
		}
	}
	/**
	 * @param	jsonReq	serialized object of JsonReqWolfpacksParams class
	 * @return	list of all social groups (wolfpacks) names, the user has access to them
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqWolfpacksParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqWolfpacksParams.class);
		} catch (Exception e) {
			return new WolfpacksResponse(null, RES_BAD_REQUEST);
		}
		
		List<WolfPack> wgroups = socialGroupsManager.getAllSocialGroups();
		List<String> groups = new ArrayList<String>();
		
		if (jsonReqParams.userID==null) {
			for (WolfPack w : wgroups) {
				groups.add(w.getName());
			}
		} else {
			try {
				UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
				Profile profile = socialFS.findProfile(uid);
				for (WolfPack w : wgroups) {
					if (w.getMembers().contains(profile)) {
						groups.add(w.getName());
					}
				}
			} catch (ProfileNotFoundException e) {
				e.printStackTrace();
				return new WolfpacksResponse(null, RES_NOT_FOUND);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return new WolfpacksResponse(null, RES_BAD_REQUEST);
			}
		}
		return new WolfpacksResponse(groups, "success");
	}
}

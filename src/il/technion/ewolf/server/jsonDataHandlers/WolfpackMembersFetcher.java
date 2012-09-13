package il.technion.ewolf.server.jsonDataHandlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.socialfs.Profile;

import static il.technion.ewolf.server.EWolfResponse.*;

public class WolfpackMembersFetcher implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;

	@Inject
	public WolfpackMembersFetcher(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
	}

	static class ProfileData {
		String name;
		String id;

		ProfileData(String name, String id) {
			this.name = name;
			this.id = id;
		}
	}

	static class WolfpackMembersResponse extends EWolfResponse {
		List<ProfileData> membersList;
		public WolfpackMembersResponse(List<ProfileData> lst) {
			this.membersList = lst;
		}

		public WolfpackMembersResponse(String result) {
			super(result);
		}
	}

	private static class JsonReqWolfpackMembersParams {
		//		If wolfpackName field wasn't sent with the request then
		//		the response list will contain all the members of all the "logged in" user wolfpacks
		String wolfpackName;
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqWolfpackMembersParams class
	 * @return	list of ProfileData objects. Each object contains name and user ID.
	 */
	@Override
	public EWolfResponse handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqWolfpackMembersParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqWolfpackMembersParams.class);
		} catch (Exception e) {
			e.printStackTrace();
			return new WolfpackMembersResponse(RES_BAD_REQUEST);
		}

		List<ProfileData> resList = new ArrayList<ProfileData>();
		List<WolfPack> wolfpacks = new ArrayList<WolfPack>();

		if (jsonReqParams.wolfpackName == null) {
			wolfpacks = socialGroupsManager.getAllSocialGroups();
		} else {
			WolfPack wp = socialGroupsManager.findSocialGroup(jsonReqParams.wolfpackName);
			if (wp == null) {
				return new WolfpackMembersResponse(RES_NOT_FOUND);
			} else {
				wolfpacks.add(wp);
			}
		}

		Set<Profile> profiles = new HashSet<Profile>();
		for (WolfPack w : wolfpacks) {
			profiles.addAll(w.getMembers());
		}

		for (Profile profile: profiles) {
			resList.add(new ProfileData(profile.getName(), profile.getUserId().toString()));
		}
		return new WolfpackMembersResponse(resList);
	}

}

package il.technion.ewolf.server.jsonDataHandlers;

import static il.technion.ewolf.server.EWolfResponse.RES_BAD_REQUEST;
import static il.technion.ewolf.server.EWolfResponse.RES_NOT_FOUND;
import static il.technion.ewolf.server.EWolfResponse.RES_SUCCESS;
import il.technion.ewolf.server.EWolfResponse;
import il.technion.ewolf.server.cache.ICacheWithParameter;
import il.technion.ewolf.socialfs.Profile;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class ProfileFetcher implements JsonDataHandler {

	private final ICacheWithParameter<Profile, String> profilesCache;

	@Inject
	public ProfileFetcher(ICacheWithParameter<Profile, String> profilesCache) {
		this.profilesCache = profilesCache;
	}

	@SuppressWarnings("unused")
	static class ProfileResponse extends EWolfResponse {
		private String name;
		private String id;

		public ProfileResponse(String name, String id, String result) {
			super(result);
			this.name = name;
			this.id = id;
		}

		public ProfileResponse(String result) {
			super(result);
		}

		public ProfileResponse(String result, String errorMessage) {
			super(result, errorMessage);
		}
	}

	private static class JsonReqProfileParams {
		//		If userID field wasn't sent with the request then
		//			the response will be for "logged in" user
		String userID;
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqProfileParams class
	 * @return	ProfileData object that contains user's name and ID
	 */
	@Override
	public EWolfResponse handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqProfileParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqProfileParams.class);
		} catch (Exception e) {
			return new ProfileResponse(RES_BAD_REQUEST);
		}

		String strUid = (jsonReqParams.userID==null) ? "-1" : jsonReqParams.userID;
		Profile profile = profilesCache.get(strUid);
		if (profile == null) {
			return new ProfileResponse(RES_NOT_FOUND, "User with given ID wasn't found.");
		}

		return new ProfileResponse(profile.getName(), profile.getUserId().toString(), RES_SUCCESS);
	}
}

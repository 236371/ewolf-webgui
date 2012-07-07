package il.technion.ewolf.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

public class ProfileFetcher implements JsonDataHandler {
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;

	@Inject
	public ProfileFetcher(SocialFS socialFS, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
	}
	
	@SuppressWarnings("unused")
	private class ProfileResponse {
		private String name;
		private String id;
		private String result;
	
		private ProfileResponse(String name, String id, String result) {
			this.name = name;
			this.id = id;
			this.result = result;
		}
	}

	private class JsonReqProfileParams {
//		If userID field wasn't sent with the request then
//			the response will be for "logged in" user
		String userID;
	}

	/**
	 * @param	jsonReq	serialized object of JsonReqProfileParams class
	 * @return	ProfileData object that contains user's name and ID
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		JsonReqProfileParams jsonReqParams;
		try {
			jsonReqParams = gson.fromJson(jsonReq, JsonReqProfileParams.class);
		} catch (Exception e) {
			return new ProfileResponse(null, null, RES_BAD_REQUEST);
		}
		
		Profile profile;
		if (jsonReqParams.userID==null) {
			profile = socialFS.getCredentials().getProfile();
			jsonReqParams.userID = profile.getUserId().toString();
		} else {
			try {
				UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
				profile = socialFS.findProfile(uid);
			} catch (ProfileNotFoundException e) {
				e.printStackTrace();
				return new ProfileResponse(null, null, RES_NOT_FOUND);
			}  catch (IllegalArgumentException e) {
				e.printStackTrace();
				return new ProfileResponse(null, null, RES_BAD_REQUEST);
			}
		}
		return new ProfileResponse(profile.getName(), jsonReqParams.userID, RES_SUCCESS);
	}
}

package il.technion.ewolf.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import static il.technion.ewolf.server.handlers.EWolfResponse.*;

public class ProfileFetcher implements JsonDataHandler {
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;

	@Inject
	public ProfileFetcher(SocialFS socialFS, UserIDFactory userIDFactory) {
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
	}
	
	@SuppressWarnings("unused")
	class ProfileResponse extends EWolfResponse {
		private String name;
		private String id;
	
		public ProfileResponse(String name, String id) {
			this.name = name;
			this.id = id;
		}

		public ProfileResponse(String result) {
			super(result);
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
			return new ProfileResponse(RES_BAD_REQUEST);
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
				return new ProfileResponse(RES_NOT_FOUND);
			}  catch (IllegalArgumentException e) {
				e.printStackTrace();
				return new ProfileResponse(RES_BAD_REQUEST);
			}
		}
		return new ProfileResponse(profile.getName(), jsonReqParams.userID);
	}
}

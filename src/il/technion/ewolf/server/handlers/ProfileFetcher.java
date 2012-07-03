package il.technion.ewolf.server.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import il.technion.ewolf.server.exceptions.NotFoundException;
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
	private class ProfileData {
		private String name;
		private String id;
	
		private ProfileData(String name, String id) {
			this.name = name;
			this.id = id;
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
	 * @throws NotFoundException
	 */
	@Override
	public Object handleData(JsonElement jsonReq) throws NotFoundException {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqProfileParams jsonReqParams = gson.fromJson(jsonReq, JsonReqProfileParams.class);
		
		Profile profile;
		if (jsonReqParams.userID==null) {
			profile = socialFS.getCredentials().getProfile();
			jsonReqParams.userID = profile.getUserId().toString();
		} else {
			UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
			try {
				profile = socialFS.findProfile(uid);
			} catch (ProfileNotFoundException e) {
				throw new NotFoundException(e);
			}
		}
		return new ProfileData(profile.getName(), jsonReqParams.userID);
	}
}

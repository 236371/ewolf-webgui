package il.technion.ewolf.server.handlers;

import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

public class SendMessageHandler implements JsonDataHandler {
	private final SocialMail smail;
	private final SocialFS socialFS;
	private final UserIDFactory userIDFactory;

	@Inject
	public SendMessageHandler(SocialMail smail, SocialFS socialFS, UserIDFactory userIDFactory) {
		this.smail = smail;
		this.socialFS = socialFS;
		this.userIDFactory = userIDFactory;
	}

	private class JsonReqSendMessageParams {
		String userID;
		//message text
		String message;
	}

	//response error messages
	private static final String PROFILE_NOT_FOUND_MESSAGE = "user not found";

	/**
	 * @param	jsonReq	serialized object of JsonReqSendMessageParams class
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(JsonElement jsonReq) {
		Gson gson = new Gson();
		//TODO handle JsonSyntaxException
		JsonReqSendMessageParams jsonReqParams =
				gson.fromJson(jsonReq, JsonReqSendMessageParams.class);

		if (jsonReqParams.message == null || jsonReqParams.userID == null) {
			return "Must specify both user ID and message body.";
		}

		UserID uid = userIDFactory.getFromBase64(jsonReqParams.userID);
		Profile profile;
		try {
			profile = socialFS.findProfile(uid);
		} catch (ProfileNotFoundException e) {
			e.printStackTrace();
			return PROFILE_NOT_FOUND_MESSAGE;
		}
		
		ContentMessage msg = smail.createContentMessage().setMessage(jsonReqParams.message);
		smail.send(msg, profile);
		return "success";
	}

}

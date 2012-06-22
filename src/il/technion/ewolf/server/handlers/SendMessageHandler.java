package il.technion.ewolf.server.handlers;

import il.technion.ewolf.msg.ContentMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

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

	/**
	 * @param	parameters	The method gets exactly 2 parameters.
	 * 						[0]:		user ID
	 * 						[1]:		post text
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(String... parameters) {
		String strUid = parameters[0];
		UserID uid = userIDFactory.getFromBase64(strUid);
		Profile profile;
		try {
			profile = socialFS.findProfile(uid);
		} catch (ProfileNotFoundException e) {
			e.printStackTrace();
			return e.toString();
		}
		String text = parameters[1];
		ContentMessage msg = smail.createContentMessage().setMessage(text);
		smail.send(msg, profile);
		return "success";
	}

}

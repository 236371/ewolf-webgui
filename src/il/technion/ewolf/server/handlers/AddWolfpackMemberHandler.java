package il.technion.ewolf.server.handlers;

import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.FileNotFoundException;

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

	/**
	 * @param	parameters	The method gets exactly 2 parameters.
	 * 						[0]:		wolfpack name
	 * 						[1]:		user ID
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(String... parameters)
			throws ProfileNotFoundException, FileNotFoundException,
			WallNotFound {
		if(parameters.length != 2) {
			return null;
		}
		
		String strUid = parameters[1];
		UserID uid = userIDFactory.getFromBase64(strUid);
		Profile profile = socialFS.findProfile(uid);
		String groupName = parameters[0];
		WolfPack socialGroup = socialGroupsManager.findSocialGroup(groupName);
		if (socialGroup == null) {
			return "wolfpack not found";
		}
		try {
			socialGroup.addMember(profile);
		} catch (GroupNotFoundException e) {
			return e.toString();
		}
		
		return "success";
	}

}

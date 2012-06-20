package il.technion.ewolf.server.handlers;


import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;
import il.technion.ewolf.socialfs.UserID;
import il.technion.ewolf.socialfs.UserIDFactory;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * @param	parameters	user ID or "my" in parameters[0]  
	 * @return	list of all social groups (wolfpacks) names, the user has access to them
	 */
	@Override
	public Object handleData(String... parameters) throws ProfileNotFoundException {
		if(parameters.length != 1) {
			return null;
		}

		String strUid = parameters[0];
		List<WolfPack> wgroups = socialGroupsManager.getAllSocialGroups();
		List<String> groups = new ArrayList<String>();

		if (strUid.equals("my")) {
			for (WolfPack w : wgroups) {
				groups.add(w.getName());
			}
		} else {
			UserID uid = userIDFactory.getFromBase64(strUid);
			Profile profile = socialFS.findProfile(uid);
			for (WolfPack w : wgroups) {
				if (w.getMembers().contains(profile)) {
					groups.add(w.getName());
				}
			}
		}
		return groups;
	}
}

package il.technion.ewolf.server.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;

import il.technion.ewolf.WolfPack;
import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.socialfs.Profile;

public class WolfpackMembersFetcher implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;

	@Inject
	public WolfpackMembersFetcher(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
	}
	
	class ProfileData {
		String name;
		String id;
	
		ProfileData(String name, String id) {
			this.name = name;
			this.id = id;
		}
	}

	/**
	 * @param	parameters	group name or "all" in parameters[0]  
	 * @return	list of ProfileData objects. Each object contains name and user ID.
	 */
	@Override
	public Object handleData(String... parameters) {
		if(parameters.length != 1) {
			return null;
		}
		List<ProfileData> lst = new ArrayList<ProfileData>();
		String socialGroupName = parameters[0];
		List<WolfPack> wolfpacks = new ArrayList<WolfPack>();
		if (socialGroupName.equals("all")) {
			wolfpacks = socialGroupsManager.getAllSocialGroups();
		} else {
			wolfpacks.add(socialGroupsManager.findSocialGroup(socialGroupName));
		}
		
		Set<Profile> profiles = new HashSet<Profile>();
		for (WolfPack w : wolfpacks) {
			profiles.addAll(w.getMembers());
		}
		
		for (Profile profile: profiles) {
			lst.add(new ProfileData(profile.getName(), profile.getUserId().toString()));
		}
		return lst;
	}

}

package il.technion.ewolf.server.fetchers;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.socialfs.Profile;

public class WolfpackMembersFetcher implements JsonDataFetcher {
	private final WolfPackLeader socialGroupsManager;

	@Inject
	public WolfpackMembersFetcher(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
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

	/**
	 * @param	parameters	group name in parameters[0]  
	 * @return	list of ProfileData objects. Each object contains name and user ID.
	 */
	@Override
	public Object fetchData(String... parameters) {
		if(parameters.length != 1) {
			return null;
		}
		String socialGroupName = parameters[0];
		
		List<Profile> profiles = socialGroupsManager.findSocialGroup(socialGroupName).getMembers();
		List<ProfileData> lst = new ArrayList<ProfileData>();
		for (Profile profile: profiles) {
			lst.add(new ProfileData(profile.getName(), profile.getUserId().toString()));
		}
		return lst;
	}

}

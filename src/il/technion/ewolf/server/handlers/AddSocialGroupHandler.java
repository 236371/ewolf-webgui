package il.technion.ewolf.server.handlers;

import il.technion.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

import java.io.FileNotFoundException;

import com.google.inject.Inject;

public class AddSocialGroupHandler implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;
	
	@Inject
	public AddSocialGroupHandler(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
	}

	/**
	 * @param	parameters	group name in parameters[0]
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(String... parameters)
			throws ProfileNotFoundException, FileNotFoundException,
			WallNotFound {
		if(parameters.length != 1) {
			return null;
		}
		String groupName = parameters[0];
		try {
			socialGroupsManager.findOrCreateSocialGroup(groupName);
		} catch (Exception e) {
			return e.toString();
		}
		return "success";
	}

}

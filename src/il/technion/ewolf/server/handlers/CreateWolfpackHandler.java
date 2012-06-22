package il.technion.ewolf.server.handlers;

import il.technion.ewolf.WolfPackLeader;

import com.google.inject.Inject;

public class CreateWolfpackHandler implements JsonDataHandler {
	private final WolfPackLeader socialGroupsManager;
	
	@Inject
	public CreateWolfpackHandler(WolfPackLeader socialGroupsManager) {
		this.socialGroupsManager = socialGroupsManager;
	}

	/**
	 * @param	parameters	wolfpack name in parameters[0]
	 * @return	"success" or error message
	 */
	@Override
	public Object handleData(String... parameters) {
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

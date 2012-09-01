package il.technion.ewolf.server;

import java.util.List;

import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMail;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import com.google.inject.Inject;

public class PokeMessagesAcceptor implements Runnable {
	private final SocialMail smail;
	private final WolfPackLeader socialGroupsManager;

	@Inject
	public PokeMessagesAcceptor(SocialMail smail, WolfPackLeader socialGroupsManager) {
		this.smail = smail;
		this.socialGroupsManager = socialGroupsManager;
	}

	@Override
	public void run() {
		try {
			while (true) {
				List<SocialMessage> messages = smail.readInbox();
				for (SocialMessage m : messages) {
					if (m.getClass() == PokeMessage.class) {
						try {
							WolfPack followers = socialGroupsManager
									.findSocialGroup("followers");
							List<Profile> followersList = followers.getMembers();
							Profile follower = m.getSender();
							if (!followersList.contains(follower)) {
								//FIXME adding to followers sends Poke message too
								followers.addMember(follower);
							}
						} catch (GroupNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ProfileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						((PokeMessage)m).accept();
						continue;
					}
				}
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			//TODO what can I do here?
		}

	}

}

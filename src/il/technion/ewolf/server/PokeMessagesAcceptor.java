package il.technion.ewolf.server;

import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.msg.PokeMessage;
import il.technion.ewolf.msg.SocialMessage;
import il.technion.ewolf.server.cache.ICache;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;

public class PokeMessagesAcceptor implements Runnable {
	private final ICache<List<SocialMessage>> inboxCache;
	private final ICache<Map<WolfPack,List<Profile>>> wolfpacksMembersCache;
	private final ICache<List<WolfPack>> wolfpacksCache;

	@Inject
	public PokeMessagesAcceptor(ICache<List<SocialMessage>> inboxCache,
			ICache<Map<WolfPack,List<Profile>>> wolfpacksMembersCache,
			ICache<List<WolfPack>> wolfpacksCache) {
		this.inboxCache = inboxCache;
		this.wolfpacksMembersCache = wolfpacksMembersCache;
		this.wolfpacksCache = wolfpacksCache;
	}

	@Override
	public void run() {
		try {
			while (true) {
				List<SocialMessage> messages = inboxCache.get();
				for (SocialMessage m : messages) {
					if (m.getClass() == PokeMessage.class) {
						Map<WolfPack,List<Profile>> wolfpacksMembersMap = wolfpacksMembersCache.get();
						List<WolfPack> wolfpacks = wolfpacksCache.get();
						List<Profile> followersList = null;
						WolfPack followers = null;
						for (WolfPack w : wolfpacks) {
							if (w.getName().equals("followers")) {
								followers = w;
								followersList = wolfpacksMembersMap.get(w);
							}
						}
						try {
							Profile follower = m.getSender();
							if (followersList != null && !followersList.contains(follower)
									&& followers != null) {
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

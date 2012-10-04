package il.technion.ewolf.server;

import il.technion.ewolf.ewolf.SocialNetwork;
import il.technion.ewolf.ewolf.WolfPack;
import il.technion.ewolf.ewolf.WolfPackLeader;
import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.posts.Post;
import il.technion.ewolf.socialfs.Profile;
import il.technion.ewolf.socialfs.SocialFS;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class ServerModule extends AbstractModule {
	private final Properties properties;

	private Properties getDefaultProperties() {
		Properties defaultProps = new Properties();

		defaultProps.setProperty("server.cache.newsfeed.intervalSec", "30");

		return defaultProps;
	}

	public ServerModule() {
		this(new Properties());
	}

	public ServerModule(Properties properties) {
		this.properties = getDefaultProperties();
		this.properties.putAll(properties);
	}

	@Override
	protected void configure() {
		Names.bindProperties(binder(), properties);
	}

	@Provides
	@Singleton
	ICache<Map<Profile,List<Post>>> provideNewsFeedCache(
			@Named("server.cache.newsfeed.intervalSec") int cachedTimeSec,
			final SocialFS socialFS,
			final WolfPackLeader socialGroupsManager,
			final SocialNetwork snet){
		return new SelfUpdatingCache<Map<Profile,List<Post>>>(
				new ICache<Map<Profile,List<Post>>>() {
					@Override
					public Map<Profile,List<Post>> get() {
						return fetchAllPosts();
					}

					private Map<Profile, List<Post>> fetchAllPosts() {
						Map<Profile, List<Post>> allPosts = new HashMap<Profile, List<Post>>();

						List<WolfPack> wolfpacks = socialGroupsManager.getAllSocialGroups();
						Set<Profile> profiles = new HashSet<Profile>();

						for (WolfPack w : wolfpacks) {
							profiles.addAll(w.getMembers());
						}
						//add self profile
						profiles.add(socialFS.getCredentials().getProfile());

						for (Profile profile: profiles) {
							try {
								List<Post> posts = snet.getWall(profile).getAllPosts();
								allPosts.put(profile, posts);
							} catch (WallNotFound e) {
								Profile user = socialFS.getCredentials().getProfile();
								System.err.println("User " + user.getName() + ": " + user.getUserId() +
										" isn't allowed to view posts of " +
										profile.getName() + ": " + profile.getUserId() + ".");
								//e.printStackTrace();
							} catch (FileNotFoundException e) {
								Profile user = socialFS.getCredentials().getProfile();
								System.err.println("User " + user.getName() + ": " + user.getUserId() +
										" isn't allowed to view posts of " +
										profile.getName() + ": " + profile.getUserId() + ".");
								//e.printStackTrace();
							}
						}
						return allPosts;
					}
				}, cachedTimeSec);
	}

}

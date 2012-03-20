package il.technion.ewolf.socialfs.background;

import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.socialfs.Credentials;

import java.util.Timer;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class ReinsertProfileTask extends AbstractBackgroundTask {
	// state
	private Credentials cred;

	// dependencies
	private final DHT profileDHT;
	
	@Inject
	ReinsertProfileTask(
			Timer timer,
			@Named("socialfs.profile.dht") DHT profileDHT) {
		
		super(timer);
		this.profileDHT = profileDHT;
	}

	@Override
	public void run() {
		try {
			Key uid = cred.getProfile().getUserId().getKey();
			//System.out.println("inserting profile for user: "+uid);
			profileDHT.put(uid, cred.getProfile());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected long getPeriod() {
		return 60000;
	}
	
	public ReinsertProfileTask setCredentials(Credentials cred) {
		this.cred = cred;
		return this;
	}

}

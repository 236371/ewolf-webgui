package il.technion.ewolf.kbr.openkad;

import il.technion.ewolf.kbr.openkad.ops.KadOperationsExecutor;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;


import com.google.inject.Inject;
import com.google.inject.name.Named;

public class KadRefresher implements Runnable {

	private final int interval;
	private final int nrBuckets;
	private final KadOperationsExecutor opExecutor;
	private final AtomicBoolean active = new AtomicBoolean(false);
	private final Random rnd;
	
	@Inject
	KadRefresher(
			@Named("kadnet.refreshinterval") int interval,
			KadOperationsExecutor opExecutor,
			@Named("kadnet.random") Random rnd,
			KBuckets kbuckets) {
		
		this.interval = interval;
		this.opExecutor = opExecutor;
		this.rnd = rnd;
		this.nrBuckets = kbuckets.getNrBuckets();
		
	}
			
	
	public void shutdown() {
		active.set(false);
	}
	
	@Override
	public void run() {
		active.set(true);
		while (active.get()) {
			try {
				Thread.sleep(interval);
				opExecutor.createBucketRefreshOperation(rnd.nextInt(nrBuckets)).call();
			} catch (Exception e) {
			}
		}
	}
	
	

}

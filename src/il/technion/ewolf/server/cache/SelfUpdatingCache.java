package il.technion.ewolf.server.cache;


public class SelfUpdatingCache<T> implements ICache<T> {
	T data;
	ICache<T> nextCache;
	int updateIntervalSeconds;

	public SelfUpdatingCache(ICache<T> nextCache, int updateIntervalSeconds) {
		this.nextCache = nextCache;
		this.updateIntervalSeconds = updateIntervalSeconds;
		new Thread(new Updater()).start();
	}
	
	@Override
	public synchronized T get() {
		return data;
	}
	
	synchronized void set(T data) {
		this.data = data;
	}

	class Updater implements Runnable {
		@Override
		public void run() {
			while(true) {
				SelfUpdatingCache.this.set(nextCache.get());
				try {
					Thread.sleep(updateIntervalSeconds*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}

package il.technion.ewolf.server.cache;



public class SimpleCache<T> implements ICache<T> {
	T data;
	ICache<T> nextCache;
	long lastModifiedMillis;
	int maxAgeSeconds;

	public SimpleCache(ICache<T> nextCache, int maxAgeSeconds) {
		this.nextCache = nextCache;
		this.maxAgeSeconds = maxAgeSeconds;
	}

	private static boolean isExpired(long lastModifiedMillis, int maxAgeSeconds) {
		long now = System.currentTimeMillis();
		return (now - lastModifiedMillis) > (maxAgeSeconds * 1000);
	}

	@Override
	public synchronized T get() {
		if (isExpired(lastModifiedMillis, maxAgeSeconds)) {
			data = nextCache.get();
			lastModifiedMillis = System.currentTimeMillis();
		}
		return data;
	}

	@Override
	public synchronized void update() {
		lastModifiedMillis = 0;
		get();
	}

}

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

	@Override
	public T get() {
		long now = System.currentTimeMillis();
		boolean expired = (now - lastModifiedMillis) > (maxAgeSeconds * 1000);
		if (expired) {
			data = nextCache.get();
			lastModifiedMillis = System.currentTimeMillis();
		}
		return data;
	}

}

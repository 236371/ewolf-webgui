package il.technion.ewolf.server.cache;



public class SimpleCacheWithParameter<T,S> implements ICacheWithParameter<T,S> {
	T data;
	ICacheWithParameter<T,S> nextCache;
	long lastModifiedMillis;
	int maxAgeSeconds;

	public SimpleCacheWithParameter(ICacheWithParameter<T,S> nextCache, int maxAgeSeconds) {
		this.nextCache = nextCache;
		this.maxAgeSeconds = maxAgeSeconds;
	}

	@Override
	public T get(S param) {
		long now = System.currentTimeMillis();
		boolean expired = (now - lastModifiedMillis) > (maxAgeSeconds * 1000);
		if (expired) {
			data = nextCache.get(param);
			lastModifiedMillis = System.currentTimeMillis();
		}
		return data;
	}

}

package il.technion.ewolf.server.cache;


public class NoCache<T> implements ICache<T> {
	T data;
	ICache<T> nextCache;

	public NoCache(ICache<T> nextCache) {
		this.nextCache = nextCache;
	}

	@Override
	public T get() {
		return nextCache.get();
	}

	@Override
	public void update() {
		get();
	}

}

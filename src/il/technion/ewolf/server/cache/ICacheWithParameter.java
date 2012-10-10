package il.technion.ewolf.server.cache;

public interface ICacheWithParameter<T,S> {

	public abstract T get(S s);

}

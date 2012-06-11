package il.technion.ewolf.server.fetchers;

import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

public interface JsonDataFetcher {
	public Object fetchData(String... parameters) throws ProfileNotFoundException;
}

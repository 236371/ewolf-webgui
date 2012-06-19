package il.technion.ewolf.server.fetchers;

import java.io.FileNotFoundException;

import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

public interface JsonDataFetcher {
	public Object fetchData(String... parameters) throws ProfileNotFoundException, FileNotFoundException, WallNotFound;
}

package il.technion.ewolf.server.handlers;

import java.io.FileNotFoundException;

import il.technion.ewolf.exceptions.WallNotFound;
import il.technion.ewolf.socialfs.exception.ProfileNotFoundException;

public interface JsonDataFetcher {
	public Object handleData(String... parameters) throws ProfileNotFoundException, FileNotFoundException, WallNotFound;
}

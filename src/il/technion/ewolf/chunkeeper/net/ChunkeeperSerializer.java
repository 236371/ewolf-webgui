package il.technion.ewolf.chunkeeper.net;

import il.technion.ewolf.chunkeeper.ChunkId;

/**
 * Serialize various chunkeeper objects for easy transfer using http
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public interface ChunkeeperSerializer {


	/**
	 * deserialize a string representation of a chunk id
	 * @param str the serialized chunk id
	 * @return the deserialized chunk id
	 */
	ChunkId fromString(String str);
	
	/**
	 * serialize a chunk id to be used as a http request query parameter
	 * @param chunkId the chunk id to be strigified
	 * @return the serialized chunk id
	 */
	String toUrlSafeString(ChunkId chunkId);
	
}

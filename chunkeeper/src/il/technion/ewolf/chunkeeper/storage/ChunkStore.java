package il.technion.ewolf.chunkeeper.storage;

import il.technion.ewolf.chunkeeper.Chunk;
import il.technion.ewolf.chunkeeper.ChunkId;
import il.technion.ewolf.kbr.Key;

/**
 * chunks storage interface 
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public interface ChunkStore {

	/**
	 * store the chunk under a given key
	 * @param key the chunk key
	 * @param data the data to be stored
	 */
	public void store(Key key, byte[] data);
	
	/**
	 * search the storage for a chunk associated with a given key
	 * @param key the data item's key
	 * @return a chunk representation of this data or null if not found
	 */
	public Chunk get(Key key);

	/**
	 * search the storage for a chunk with the given chunk id
	 * @param chunkId the requested chunk id
	 * @return a chunk representation of this data or null if not found
	 */
	public Chunk get(ChunkId chunkId);
	
}

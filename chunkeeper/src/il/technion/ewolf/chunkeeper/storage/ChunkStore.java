package il.technion.ewolf.chunkeeper.storage;

import il.technion.ewolf.chunkeeper.Chunk;
import il.technion.ewolf.chunkeeper.ChunkId;
import il.technion.ewolf.kbr.Key;

public interface ChunkStore {

	public void store(Key key, byte[] data);
	
	public Chunk get(Key key);

	public Chunk get(ChunkId chunkId);
	
}

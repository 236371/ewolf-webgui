package il.technion.ewolf.chunkeeper.net;

import il.technion.ewolf.chunkeeper.ChunkId;

public interface ChunkeeperSerializer {


	ChunkId fromString(String str);
	
	String toUrlSafeString(ChunkId chunkId);
	
}

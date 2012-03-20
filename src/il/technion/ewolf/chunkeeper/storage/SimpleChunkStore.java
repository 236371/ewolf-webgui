package il.technion.ewolf.chunkeeper.storage;

import il.technion.ewolf.chunkeeper.Chunk;
import il.technion.ewolf.chunkeeper.ChunkId;
import il.technion.ewolf.chunkeeper.ChunkLocator;
import il.technion.ewolf.dht.DHT;
import il.technion.ewolf.kbr.Key;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * Stores the chunk in memory
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class SimpleChunkStore implements ChunkStore {

	private final DHT chucksDHT;
	private final String[] hashAlgos;
	private final Provider<Chunk> chunkProvider;
	private final Provider<ChunkLocator> chunkLocatorProvider;
	
	private final Map<Key, ChunkId> chunkIdFromKey;
	private final Map<ChunkId, byte[]> dataFromChunkId;
	
	@Inject
	SimpleChunkStore(
			@Named("chunkeeper.dht") DHT chucksDHT,
			@Named("chunkeeper.store.hashalgos") String[] hashAlgos,
			Provider<Chunk> chunkProvider,
			Provider<ChunkLocator> chunkLocatorProvider) {
		
		this.chucksDHT = chucksDHT;
		this.hashAlgos = hashAlgos;
		this.chunkProvider = chunkProvider;
		this.chunkLocatorProvider = chunkLocatorProvider;
		
		chunkIdFromKey = new HashMap<Key, ChunkId>();
		dataFromChunkId = new HashMap<ChunkId, byte[]>();
	}
	
	@Override
	public synchronized void store(Key key, byte[] data) {
		if (chunkIdFromKey.containsKey(key)) {
			throw new IllegalArgumentException("already have a chunk with this key");
		}
		ChunkId chunkId = new ChunkId(key, data, hashAlgos);
		chunkIdFromKey.put(key, chunkId);
		dataFromChunkId.put(chunkId, data);
		
		try {
			chucksDHT.put(key, chunkLocatorProvider.get().setChunkId(chunkId));
		} catch (Exception e) {
			System.out.println("could not publish in dht");
			e.printStackTrace();
		}
	}

	@Override
	public synchronized Chunk get(Key key) {
		ChunkId chunkId = chunkIdFromKey.get(key);
		if (chunkId == null)
			return null;
		Chunk chunk = chunkProvider.get();
		
		chunk
			.setData(dataFromChunkId.get(chunkId))
			.addLocator(chunkLocatorProvider.get().setChunkId(chunkId));
		
		return chunk;
	}

	@Override
	public Chunk get(ChunkId chunkId) {
		Chunk chunk = get(chunkId.getKey());
		if (chunk.getChunkId().equals(chunkId))
			return chunk;
		return null;
	}

}

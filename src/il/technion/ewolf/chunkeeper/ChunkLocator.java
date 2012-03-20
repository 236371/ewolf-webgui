package il.technion.ewolf.chunkeeper;

import il.technion.ewolf.kbr.Node;

import java.io.Serializable;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Represent a location of a chunk in the key based routing network 
 * @author eyal.kibbar@gmail.com
 *
 */
public class ChunkLocator implements Serializable {

	private static final long serialVersionUID = 5874006247051711061L;

	private ChunkId chunkId;
	private final Node holder;
	
	@Inject
	ChunkLocator(@Named("openkad.local.node") Node localNode) {
		this.holder = localNode;
	}
	
	/**
	 * 
	 * @return this chunk id
	 */
	public ChunkId getChunkId() {
		return chunkId;
	}
	
	/**
	 * 
	 * @return the node holding this chunk
	 */
	public Node getHolder() {
		return holder;
	}
	
	public ChunkLocator setChunkId(ChunkId chunkId) {
		this.chunkId = chunkId;
		return this;
	}
	
}

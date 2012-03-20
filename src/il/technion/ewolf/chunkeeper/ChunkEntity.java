package il.technion.ewolf.chunkeeper;

public class ChunkEntity {

	private ChunkId chunkId;
	private byte[] data;
	
	ChunkEntity() {
		
	}
	
	public ChunkId getChunkId() {
		return chunkId;
	}
	
	public void setChunkId(ChunkId chunkId) {
		this.chunkId = chunkId;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
}

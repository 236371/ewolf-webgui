package il.technion.ewolf.stash;

import il.technion.ewolf.chunkeeper.Chunk;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Collection;

import com.google.inject.Inject;

public class LazyChunkDecryptor {

	Stash stash;
	
	Chunk chunk;
	
	@Inject
	LazyChunkDecryptor(Stash stash) {
		this.stash = stash;
	}
	
	
	LazyChunkDecryptor setChunk(Chunk chunk) {
		this.chunk = chunk;
		return this;
	}
	
	void addTo(Collection<LazyChunkDecryptor> c) {
		c.add(this);
	}
	
	
	@SuppressWarnings("unchecked")
	public <T> T downloadAndDecrypt(Class<T> clazz) throws IOException, ClassNotFoundException, ClassCastException, GroupNotFoundException, InvalidKeyException {
		EncryptedChunk encChunk = (EncryptedChunk)chunk.download();
		Group group = stash.getGroupFromId(encChunk.getGroupId());
		Object $ = group.decrypt(encChunk.getData());
		if (!$.getClass().isAssignableFrom(clazz)) {
			throw new ClassCastException();
		}
		return (T)$;
	}
	
}

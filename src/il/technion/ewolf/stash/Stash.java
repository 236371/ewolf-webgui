package il.technion.ewolf.stash;

import il.technion.ewolf.chunkeeper.ChunKeeper;
import il.technion.ewolf.chunkeeper.Chunk;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.stash.crypto.EncryptedObject;
import il.technion.ewolf.stash.exception.GroupNotFoundException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class Stash {

	// dependencies
	private final ChunKeeper chunkeeper;
	private final Provider<Group> groupProvider;
	private final KeyFactory keyFactory;
	private Provider<LazyChunkDecryptor> lazyChunkDecryptorProvider;
	// state
	private SecretKey groupMasterKey = null;
	
	// optimizations
	private Map<Key, Group> groupsCache = new ConcurrentHashMap<Key, Group>();
	
	@Inject
	Stash(
			ChunKeeper chunkeeper,
			Provider<Group> groupProvider,
			KeyFactory keyFactory,
			Provider<LazyChunkDecryptor> lazyChunkDecryptorProvider) {
		
		this.chunkeeper = chunkeeper;
		this.groupProvider = groupProvider;
		this.keyFactory = keyFactory;
		this.lazyChunkDecryptorProvider = lazyChunkDecryptorProvider;
	}
	
	public void login(SecretKey groupMasterKey) {
		this.groupMasterKey = groupMasterKey;
	}
	
	public void put(Key key, Serializable obj, Group group) {
		chunkeeper.store(key, group.encrypt(obj));
	}
	
	public List<LazyChunkDecryptor> get(Key key) {
		Set<Chunk> chunks = chunkeeper.findChunk(key);
		
		List<LazyChunkDecryptor> $ = new ArrayList<LazyChunkDecryptor>();
		
		for (final Chunk c : chunks) {
			lazyChunkDecryptorProvider.get()
				.setChunk(c)
				.addTo($);
		}
		return $;
	}
	
	
	public Group createGroup() {
		Group g = groupProvider.get();
		addGroup(g);
		return g;
	}
	
	
	public List<Group> getAllGroups() {
		List<Group> $ = new ArrayList<Group>();
		
		for (int i=0; ; ++i) {
		
			Key groupKey = keyFactory.create(
					Base64.encodeBase64String(groupMasterKey.getEncoded()),
					"groups",
					Integer.toString(i));
			
			Set<Chunk> chnuks = chunkeeper.findChunk(groupKey);
			
			List<Group> g = new ArrayList<Group>();
			for (Chunk c : chnuks) {
				try {
					@SuppressWarnings("unchecked")
					EncryptedObject<Group> encGroup = (EncryptedObject<Group>)c.download();
					
					g.add(encGroup.decrypt(groupMasterKey));
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			$.addAll(g);
			
			if (g.isEmpty()) {
				return $;
			}
		}
		
	}
	
	public void addGroup(Group g) {
		if (g.getGroupId() == null || g.getGroupSecretKey() == null)
			throw new IllegalArgumentException("group is incomplete");
		
		try {
			getGroupFromId(g.getGroupId());
			return; // group already exists
		} catch (GroupNotFoundException e1) {
		}
		
		int nrGroups = getAllGroups().size();
		Key newGroupKey = keyFactory.create(
				Base64.encodeBase64String(groupMasterKey.getEncoded()),
				"groups",
				Integer.toString(nrGroups));
		
		chunkeeper.store(newGroupKey, new EncryptedObject<Group>().encrypt(g, groupMasterKey));
		groupsCache.put(g.getGroupId(), g);
	}

	public Group getGroupFromId(Key groupId) throws GroupNotFoundException {
		Group $ = groupsCache.get(groupId);
		if ($ != null)
			return $;
		for (Group g : getAllGroups()) {
			groupsCache.put(g.getGroupId(), g);
			if (groupId.equals(g.getGroupId()))
				return g;
		}
		throw new GroupNotFoundException("group id: "+groupId);
	}
}

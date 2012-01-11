package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class FindNodeRequest extends KadRequest {

	private static final long serialVersionUID = -7084922793331210968L;
	private Key key;
	private int maxNodes;
	private boolean searchCache;
	
	@Inject
	FindNodeRequest(
			@Named("openkad.rnd.id") long id,
			@Named("openkad.local.node") Node src) {
		super(id, src);
	}
	
	public Key getKey() {
		return key;
	}
	
	public FindNodeRequest setKey(Key key) {
		this.key = key;
		return this;
	}

	@Override
	public FindNodeResponse generateResponse(@Named("openkad.local.node") Node localNode) {
		return new FindNodeResponse(getId(), localNode);
	}

	public int getMaxNodes() {
		return maxNodes;
	}
	
	public FindNodeRequest setSearchCache(boolean searchCache) {
		this.searchCache = searchCache;
		return this;
	}
	
	public boolean shouldSearchCache() {
		return searchCache;
	}
	
	public FindNodeRequest setMaxNodes(int maxNodes) {
		this.maxNodes = maxNodes;
		return this;
	}

}

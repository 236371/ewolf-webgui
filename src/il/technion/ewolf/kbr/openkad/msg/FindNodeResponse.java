package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Node;

import java.util.List;

import com.google.inject.name.Named;

/**
 * A findNode response as defined in the kademlia protocol
 * @author eyal.kibbar@gmail.com
 *
 */
public class FindNodeResponse extends KadResponse {

	private static final long serialVersionUID = 2103126060969733458L;
	private List<Node> nodes;
	private boolean cachedResults;
	
	FindNodeResponse(long id, @Named("openkad.local.node") Node src) {
		super(id, src);
	}
	
	public FindNodeResponse setNodes(List<Node> nodes) {
		this.nodes = nodes;
		return this;
	}
	
	/**
	 * 
	 * @return the nodes closest to the request key
	 */
	public List<Node> getNodes() {
		return nodes;
	}
	
	public FindNodeResponse setCachedResults(boolean cachedResults) {
		this.cachedResults = cachedResults;
		return this;
	}
	
	public boolean isCachedResults() {
		return cachedResults;
	}

}

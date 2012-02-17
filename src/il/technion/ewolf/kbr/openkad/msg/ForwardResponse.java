package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Node;

import java.util.List;

/**
 * A forward response as defined in the colors protocol
 * TODO: add a link to the published article
 * @author eyal.kibbar@gmail.com
 *
 */
public class ForwardResponse extends KadResponse {

	private static final long serialVersionUID = 6325079396969335098L;
	
	
	private List<Node> nodes = null;
	private boolean ack;
	
	ForwardResponse(long id, Node src) {
		super(id, src);
	}

	public List<Node> getNodes() {
		return nodes;
	}
	
	
	public ForwardResponse setNodes(List<Node> nodes) {
		this.nodes = nodes;
		return this;
	}
	
	
	public ForwardResponse setAck() {
		this.ack = true;
		return this;
	}
	
	public ForwardResponse setNack() {
		this.ack = false;
		return this;
	}
	
	
	public boolean isAck() {
		return ack;
	}
	
	public boolean isNack() {
		return !ack;
	}
	

}

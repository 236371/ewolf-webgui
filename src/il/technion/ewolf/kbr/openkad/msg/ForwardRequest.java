package il.technion.ewolf.kbr.openkad.msg;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.Node;

import java.util.List;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * A forward request as defined in the colors protocol
 * TODO: add a link to the published article
 * @author eyal.kibbar@gmail.com
 *
 */
public class ForwardRequest extends KadRequest {

	private static final long serialVersionUID = -1087198782219829035L;

	private Key key;
	private List<Node> bootstrap;
	
	//TODO: testing only REMOVE B4 PUBLISH
	private boolean isInitiator = false;
	
	@Inject
	ForwardRequest(
			@Named("openkad.rnd.id") long id,
			@Named("openkad.local.node") Node src) {
		super(id, src);
	}


	public List<Node> getBootstrap() {
		return bootstrap;
	}
	
	public Key getKey() {
		return key;
	}
	
	public ForwardRequest setBootstrap(List<Node> bootstrap) {
		this.bootstrap = bootstrap;
		return this;
	}
	
	
	public ForwardRequest setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public ForwardMessage generateMessage(Node localNode) {
		return new ForwardMessage(getId(), localNode);
	}
	

	@Override
	public ForwardResponse generateResponse(Node localNode) {
		return new ForwardResponse(getId(), localNode);
	}
	
	//TODO: remove b4 publish
	public ForwardRequest setInitiator() {
		this.isInitiator = true;
		return this;
	}
	
	public boolean isInitiator() {
		return isInitiator;
	}

}

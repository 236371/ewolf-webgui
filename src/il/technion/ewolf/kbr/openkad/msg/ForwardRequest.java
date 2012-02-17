package il.technion.ewolf.kbr.openkad.msg;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import static org.hamcrest.Matchers.is;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyComparator;
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
	
	public ForwardRequest mergeBootstraps(List<Node> bootstrap, int maxSize) {
		bootstrap.removeAll(this.bootstrap);
		this.bootstrap.addAll(bootstrap);
		if (this.bootstrap.isEmpty())
			return this;
		
		this.bootstrap = sort(this.bootstrap, on(Node.class).getKey(), new KeyComparator(key));
		if (this.bootstrap.size() > maxSize)
			this.bootstrap.subList(maxSize, this.bootstrap.size()).clear();
		
		return this;
	}
	
	public Node calcNextHop(int nrColors) {
		Node $ = bootstrap.get(0);
		
		//List<Node> colorSorted = sort(bootstrap, on(Node.class).getKey(), new KeyColorComparator(key, nrColors));
		
		int keyColor = key.getColor(nrColors);
		
		List<Node> correctColorNodes = filter(
				having(on(Node.class).getKey().getColor(nrColors), is(keyColor)),
				bootstrap);

		if (!correctColorNodes.isEmpty()) {
			return correctColorNodes.get(0);
		}
		
		/*
		if (colorSorted.get(0).getKey().getColor(nrColors) == key.getColor(nrColors))
			$ = colorSorted.get(0);
		*/
		KeyComparator comparator = new KeyComparator(key);
		
		return comparator.compare(getSrc().getKey(), $.getKey()) <= 0 ? null : $;
	}
	
	public ForwardRequest setKey(Key key) {
		this.key = key;
		return this;
	}
	
	public ForwardRequest setPreviousRequest(ForwardRequest req) {
		return this.setKey(req.getKey())
			.setBootstrap(req.getBootstrap());
		
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

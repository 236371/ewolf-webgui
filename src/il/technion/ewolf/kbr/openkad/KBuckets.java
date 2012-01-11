package il.technion.ewolf.kbr.openkad;

import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.sort;
import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.KeyColorComparator;
import il.technion.ewolf.kbr.KeyComparator;
import il.technion.ewolf.kbr.KeyFactory;
import il.technion.ewolf.kbr.Node;
import il.technion.ewolf.kbr.concurrent.CompletionHandler;
import il.technion.ewolf.kbr.openkad.msg.FindNodeResponse;
import il.technion.ewolf.kbr.openkad.msg.KadMessage;
import il.technion.ewolf.kbr.openkad.msg.PingResponse;
import il.technion.ewolf.kbr.openkad.net.MessageDispatcher;
import il.technion.ewolf.kbr.openkad.net.filter.SrcExcluderMessageFilter;
import il.technion.ewolf.kbr.openkad.net.filter.TypeExcluderMessageFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class KBuckets {

	private final Provider<MessageDispatcher<Object>> msgDispatcherProvider;
	private final Provider<KadNode> kadNodeProvider;
	private final Bucket[] kbuckets;
	private final Bucket[] colorBuckets;
	private final Bucket[] slackBuckets;
	private final Node localNode;
	private final KeyFactory keyFactory;
	private final int colorBucketSize;
	private final int nrColors;
	@Inject
	KBuckets(
			KeyFactory keyFactory,
			Provider<KadNode> kadNodeProvider,
			Provider<MessageDispatcher<Object>> msgDispatcherProvider,
			@Named("openkad.bucket.colors.nrcolors") int nrColors,
			@Named("openkad.bucket.colors.maxsize") int colorBucketSize,
			@Named("openkad.bucket.colors") Provider<Bucket> colorsProvider,
			@Named("openkad.bucket.kbuckets") Provider<Bucket> kBucketProvider,
			@Named("openkad.bucket.slack") Provider<Bucket> provideSlackBucket,
			@Named("openkad.local.node") Node localNode) {
		
		this.keyFactory = keyFactory;
		this.msgDispatcherProvider = msgDispatcherProvider;
		this.kadNodeProvider = kadNodeProvider;
		this.localNode = localNode;
		this.colorBucketSize = colorBucketSize;
		this.nrColors = nrColors;
		
		kbuckets = new Bucket[keyFactory.getBitLength()];
		for (int i=0; i < kbuckets.length; ++i) {
			kbuckets[i] = kBucketProvider.get();
		}
		
		colorBuckets = new Bucket[nrColors];
		for (int i=0; i < colorBuckets.length; ++i) {
			colorBuckets[i] = colorsProvider.get();
		}
		
		slackBuckets = new Bucket[nrColors];
		for (int i=0; i < slackBuckets.length; ++i) {
			slackBuckets[i] = provideSlackBucket.get();
		}
		
	}

	public List<Key> randomKeysForAllBuckets() {
		List<Key> $ = new ArrayList<Key>();
		for (int i=0; i < kbuckets.length; ++i) {
			Key key = keyFactory.generate(i).xor(localNode.getKey());
			$.add(key);
		}
		return $;
	}
	
	public void registerIncomingMessageHandler() {
		msgDispatcherProvider.get()
			.setConsumable(false)
			// do not add PingResponse since it might create a loop
			.addFilter(new TypeExcluderMessageFilter(PingResponse.class))
			.addFilter(new SrcExcluderMessageFilter(localNode))
			
			.setCallback(null, new CompletionHandler<KadMessage, Object>() {
				
				@Override
				public void failed(Throwable exc, Object attachment) {
					// should never be here
					exc.printStackTrace();
				}
				
				@Override
				public void completed(KadMessage msg, Object attachment) {
					KBuckets.this.insert(kadNodeProvider.get()
							.setNode(msg.getSrc())
							.setNodeWasContacted());
					
					if (msg instanceof FindNodeResponse) {
						for (Node n : ((FindNodeResponse)msg).getNodes()) {
							KBuckets.this.insert(kadNodeProvider.get().setNode(n));
						}
					}
				}
			})
			.register();
	}
	
	public List<Node> getAllNodes() {
		List<Node> $ = new ArrayList<Node>();
		for (int i=0; i < kbuckets.length; ++i) {
			kbuckets[i].addNodesTo($);
		}
		return $;
	}
	
	
	private int getKBucketIndex(Key key) {
		return key.xor(localNode.getKey()).getFirstSetBitIndex();
	}
	
	public void insert(KadNode node) {
		int i = getKBucketIndex(node.getNode().getKey());
		if (i == -1)
			return;
		
		kbuckets[i].insert(node);
		
		int colorIndex = node.getNode().getKey().getColor(nrColors);
		colorBuckets[colorIndex].insert(node);
		slackBuckets[colorIndex].insert(node);
	}
	
	public List<Node> getClosestNodesFromKBuckets(Key k, int n) {
		List<Node> $ = getClosestNodes(k, n, getKBucketIndex(k), kbuckets);
		if ($.isEmpty())
			return $;
		$ = sort($, on(Node.class).getKey(), new KeyComparator(k));
		if ($.size() > n)
			$.subList(n, $.size()).clear();
		return $;
	}
	
	
	public List<Node> getClosestNodesFromColors(Key k, int n) {
		//List<Node> $ = getClosestNodes(k, n, k.getColor(nrColors), colorBuckets);
		//List<Node> $ = getClosestNodes(k, n, k.getColor(nrColors), slackBuckets);
		//$.addAll(slackNodes);
		//List<Node> knodes = getClosestNodes(k, n, getKBucketIndex(k), kbuckets, $);
		//$.addAll(knodes);
		
		List<Node> $ = getClosestNodes(k, n, getKBucketIndex(k), kbuckets);
		
		$ = sort($, on(Node.class).getKey(), new KeyColorComparator(k, nrColors));
		if ($.size() > n)
			$.subList(n, $.size()).clear();
		
		return $;
	}
	
	private List<Node> getClosestNodes(Key k, int n, int index, Bucket[] buckets) {
		Set<Node> emptySet = Collections.emptySet();
		return getClosestNodes(k, n, index, buckets, emptySet);
	}
	
	private List<Node> getClosestNodes(Key k, int n, int index, Bucket[] buckets, Collection<Node> exclude) {
	
		final List<Node> $ = new ArrayList<Node>();
		final Set<Node> t = new HashSet<Node>();
		if (index < 0)
			index = 0;
		
		buckets[index].addNodesTo($);
		
		if ($.size() < n) {
			// look in other buckets
			for (int i=1; $.size() < n; ++i) {
				if (index + i < buckets.length) {
					buckets[index + i].addNodesTo(t);
					t.removeAll(exclude);
					$.addAll(t);
					t.clear();
				}
			
				if (0 <= index - i) {
					buckets[index - i].addNodesTo(t);
					t.removeAll(exclude);
					$.addAll(t);
					t.clear();
				}
				
				if (buckets.length <= index + i && index - i < 0)
					break;
			}
		}
		
		return $;
	}
	
	@Override
	public String toString() {
		String $ = "";
		for (int i=0; i < kbuckets.length; ++i)
			$ += kbuckets[i].toString()+"\n";
		return $;
	}
	
}

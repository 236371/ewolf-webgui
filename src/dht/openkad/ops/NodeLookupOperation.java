package dht.openkad.ops;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dht.Key;
import dht.openkad.KBucketsList;
import dht.openkad.KadConnection;
import dht.openkad.KadMsg;
import dht.openkad.KadMsg.RPC;
import dht.openkad.KadMsgBuilder;
import dht.openkad.KadNode;
import dht.openkad.KadNodeComparator;
import dht.openkad.validator.KadBasicMsgValidator;
import dht.openkad.validator.KadMsgValidator;
import dht.openkad.validator.KadRPCValidator;

class NodeLookupOperation extends KadOperation<List<KadNode>> {

	private final Key key;
	private final int alpha;
	private final KadOperationsExecutor opExecutor;
	private final KadMsg findNodeMsg;
	private final Comparator<KadNode> closestToKeyComparator;
	private final KadMsgValidator basicValidator;
	
	
	public NodeLookupOperation(KadNode localNode,
			KBucketsList kbuckets,
			int concurrencyFactor,
			KadOperationsExecutor opExecutor,
			Key key,
			KadBasicMsgValidator validator) {
		
		super(localNode, kbuckets);
		this.key = key;
		this.alpha = concurrencyFactor;
		this.opExecutor = opExecutor;
		this.findNodeMsg = new KadMsgBuilder()
			.setRpc(RPC.FIND_NODE)
			.setSrc(getLocalNode())
			.setKey(key)
			.buildMessage();
		this.closestToKeyComparator = new KadNodeComparator(key);
		this.basicValidator = validator;
	}
	
	

	private List<KadNode> getAlphaNotQueried(Collection<KadNode> queried, List<KadNode> knownClosestNodes) {
		List<KadNode> $ = new ArrayList<KadNode>();
		int k = getKbucketsList().getBucketSize();
		for (int i=0; $.size() < alpha && i < k && i < knownClosestNodes.size(); ++i) {
			if (!queried.contains(knownClosestNodes.get(i)))
				$.add(knownClosestNodes.get(i));
		}
		return $;
	}
	
	private KadConnection[] sendMessageToAll(List<KadNode> nodes) {
		 
		
		KadConnection[] $ = new KadConnection[nodes.size()];
		Arrays.fill($, null);
		for (int i=0; i < $.length; ++i) {
			try {
				KadConnection conn = nodes.get(i).openConnection();
				conn.sendMessage(findNodeMsg);
				$[i] = conn;
			} catch (IOException e) {
				System.err.println("warning: unable to send FIND_NODE request to "+nodes.get(i)+": "+e);
			}
		}
		return $;
	}
	

	
	@Override
	public List<KadNode> call() {
		final int k = getKbucketsList().getBucketSize();
		List<KadNode> knownClosestNodes = getKbucketsList().getKClosestNodes(key);
		Set<KadNode> alreadyQueriedNodes = new HashSet<KadNode>();
		alreadyQueriedNodes.add(getLocalNode());
		List<KadNode> queryThisRound;
		
		while (!(queryThisRound = getAlphaNotQueried(alreadyQueriedNodes, knownClosestNodes)).isEmpty()) {
			
			/* TODO: remove this 
			if (getLocalNode().getKey().toBase64().equals("WA==")) { 
				System.out.println("knownClosestNodes: "+knownClosestNodes);
				System.out.println("queryThisRound: " + queryThisRound);
				System.out.println("alreadyQueriedNodes: "+alreadyQueriedNodes);
				
			}
			*/
			alreadyQueriedNodes.addAll(queryThisRound);
			
			
			
			// send FIND_NODE request to all nodes in queryThisRound
			KadConnection[] conns = sendMessageToAll(queryThisRound);
			
			for (int i=0; i < conns.length; ++i) {
				try {
					KadMsg recvedMsg = conns[i].recvMessage(basicValidator, KadRPCValidator.findNodeValidator);
					/* TODO: remove this 
					if (getLocalNode().getKey().toBase64().equals("WA==")) { 
						System.out.println(queryThisRound.get(0)+" knows about "+recvedMsg.getKnownClosestNodes());
						System.out.println();
					}
					*/
					// insert the node we just heard from
					opExecutor.executeInsertNode(recvedMsg.getSrc());
					
					// insert all the nodes my contact told me about
					opExecutor.executeInsertNodeIfNotFull((KadNode[])recvedMsg.getKnownClosestNodes().toArray());
					
					// only add nodes that have not been queried already and are not
					// scheduled to be queried
					Collection<KadNode> toAdd = new HashSet<KadNode>(recvedMsg.getKnownClosestNodes());
					toAdd.removeAll(alreadyQueriedNodes);
					toAdd.removeAll(knownClosestNodes);
					
					knownClosestNodes.addAll(toAdd);
					
					Collections.sort(knownClosestNodes, closestToKeyComparator);
					// remove the x-tra nodes
					for (int j=knownClosestNodes.size()-1; j >= k; knownClosestNodes.remove(j--));
					
				} catch (Exception e) {
					knownClosestNodes.remove(queryThisRound.get(i));
					
				} finally {
					if (conns[i] != null)
						conns[i].close();
				}
			}
			//Collections.sort(knownClosestNodes, closestToKeyComparator);
		}
		
		/*
		if (knownClosestNodes.size() > k)
			knownClosestNodes = new ArrayList<KadNode>(knownClosestNodes.subList(0, k));
		*/
		/* TODO: remove this 
		if (getLocalNode().getKey().toBase64().equals("WA==")) { 
			System.out.println("knownClosestNodes: "+knownClosestNodes);
			System.out.println("***");
		}
		*/
		return knownClosestNodes;
	}

}

package il.technion.ewolf.kbr.openkad.ops;

import il.technion.ewolf.kbr.Key;
import il.technion.ewolf.kbr.openkad.KBuckets;
import il.technion.ewolf.kbr.openkad.KadKeyComparator;
import il.technion.ewolf.kbr.openkad.KadMessage;
import il.technion.ewolf.kbr.openkad.KadMessage.RPC;
import il.technion.ewolf.kbr.openkad.KadMessageBuilder;
import il.technion.ewolf.kbr.openkad.KadNode;
import il.technion.ewolf.kbr.openkad.OpenedKadConnections;
import il.technion.ewolf.kbr.openkad.net.KadConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


class NodeLookupOperation extends KadOperation<List<KadNode>>  {

	private final KadNode localNode;
	private final KBuckets kbuckets;
	private final KadOperationsExecutor opExecutor;
	private final int concurrency;
	private final Key key;
	private final KadKeyComparator nodeComparator;
	private final OpenedKadConnections openedKadConnections;
	private final int maxNodeCount;
	
	NodeLookupOperation(
			Logger logger,
			KadNode localNode,
			OpenedKadConnections openedKadConnections,
			KBuckets kbuckets,
			KadOperationsExecutor opExecutor,
			int concurrency,
			Key key,
			int maxNodeCount) {
		super(logger);
		this.localNode = localNode;
		this.kbuckets = kbuckets;
		this.opExecutor = opExecutor;
		this.concurrency = concurrency;
		this.key = key;
		this.nodeComparator = new KadKeyComparator(key);
		this.maxNodeCount = maxNodeCount;
		this.openedKadConnections = openedKadConnections;
	}
	
	
	private List<KadNode> getNodesToQuery(Set<KadNode> queriedNodes, List<KadNode> knownClosestNodes) {
		List<KadNode> $ = new ArrayList<KadNode>();
		for (int i=0; $.size() < concurrency && i < knownClosestNodes.size(); ++i) {
			if (!queriedNodes.contains(knownClosestNodes.get(i)))
				$.add((KadNode)knownClosestNodes.get(i));
		}
		return $;
	}
	
	private List<List<KadConnection>> getConnections(List<KadNode> nodesToConnect) {
		List<List<KadConnection>> $ = new ArrayList<List<KadConnection>>();
		for (int i=0; i < nodesToConnect.size(); ++i) {
			List<KadConnection> conns = nodesToConnect.get(i).getKadConnections();
			$.add(conns);
			logger.info("node "+nodesToConnect.get(i)+" has "+conns.size()+" connections");
		}
		
		return $;
	}
	
	private void sendFindNode(List<List<KadConnection>> connections, List<KadNode> nodes) {
		for (int i=0; i < nodes.size(); ++i) {
			
			List<KadConnection> connList = connections.get(i);
			KadNode dstNode = nodes.get(i);
			
			while (!connList.isEmpty()) {
				KadConnection conn = connList.get(0);
				try {
					new KadMessageBuilder()
						.addHop(localNode)
						.addContacts(openedKadConnections.getAllContacts())
						.setRpc(RPC.FIND_NODE)
						.setDst(dstNode.getKey())
						.setKey(key)
						.setMaxNodeCount(Math.max(concurrency, maxNodeCount))
						.sendTo(conn);
					break;
				} catch (Exception e) {
					logger.warning("error sending message to "+dstNode.getKey()+", try the next connection");
					conn.close();
					connList.remove(0);
				}
			}
			if (connList.isEmpty())
				logger.warning("error sending message to "+dstNode.getKey()+", no more connection to try");
		}
	}
	
	private boolean hasMoreConnections(List<List<KadConnection>> connections) {
		for (List<KadConnection> connList : connections) {
			if (!connList.isEmpty())
				return true;
		}
		return false;
	}
	
	private void closeAllConnections(List<KadConnection> conns) {
		for (KadConnection c : conns) {
			c.close();
		}
		conns.clear();
	}
	
	private void recvFindNode(
			List<List<KadConnection>> connections,
			Set<KadNode> queriedNodes,
			List<KadNode> knownClosestNodes) {
		
		for (List<KadConnection> connList : connections) {
			if (connList.isEmpty())
				continue;
			
			KadConnection conn = connList.remove(0);
			try {
				KadMessage recved = conn.recvMessage();
				logger.info("recved message from "+recved.getFirstHop().getKey());
				logger.info(recved.getFirstHop().getKey()+" find nodes: "+recved.getNodes());
				// clearing the list marks it as finished work
				closeAllConnections(connList);
				
				// insert the source node
				opExecutor.executeInsertNodeOperation(recved.getLastHop());
				opExecutor.executeInsertNodeIfNotFullOperation(recved.getNodes());
				
				// add all new nodes to the knownClosestNodes
				Set<KadNode> nodesToAdd = new HashSet<KadNode>(recved.getNodes());
				//System.err.println("nodesToAdd: "+nodesToAdd);
				nodesToAdd.removeAll(queriedNodes);
				nodesToAdd.removeAll(knownClosestNodes);
				
				knownClosestNodes.addAll(nodesToAdd);
				
				// keep only the k closest nodes
				Collections.sort(knownClosestNodes, nodeComparator);
				for (int i=knownClosestNodes.size()-1; i >= Math.max(concurrency, maxNodeCount); --i)
					knownClosestNodes.remove(i);
				
				
			} catch (Exception e) {
				logger.warning("error recving message from "+conn+", try the next connection");
			} finally {
				conn.close();
			}
		}
	}
	
	@Override
	public List<KadNode> call() throws Exception {
		logger.info("starting node lookup for "+key);
		Set<KadNode> queriedNodes = new HashSet<KadNode>();
		queriedNodes.add(localNode);
		
		List<KadNode> knownClosestNodes = new ArrayList<KadNode>(
				kbuckets.getKClosestNodes(key, queriedNodes, Math.max(concurrency, maxNodeCount)));
		knownClosestNodes.add(localNode);
		
		List<KadNode> nodesToQuery;
		
		while (!(nodesToQuery = getNodesToQuery(queriedNodes, knownClosestNodes)).isEmpty()) {
			//System.err.println("knownClosestNodes: "+knownClosestNodes);
			//System.err.println("nodesToQuery: "+nodesToQuery);
			//System.err.println("queriedNodes: "+queriedNodes);
			
			queriedNodes.addAll(nodesToQuery);
			logger.info("quering nodes "+nodesToQuery);
			logger.info("known closest nodes: "+knownClosestNodes);
			List<List<KadConnection>> connections = getConnections(nodesToQuery);
			
			while (hasMoreConnections(connections)) {
				sendFindNode(connections, nodesToQuery);
				recvFindNode(connections, queriedNodes, knownClosestNodes);
			}
			
			//System.err.println("");
		}
		
		//System.err.println("knownClosestNodes: "+knownClosestNodes);
		//System.err.println("queriedNodes: "+queriedNodes);
		
		// remove x-tra nodes
		for (int i=knownClosestNodes.size()-1; i >= maxNodeCount; --i)
			knownClosestNodes.remove(i);
		return knownClosestNodes;
	}

}

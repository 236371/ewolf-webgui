package il.technion.ewolf.kbr;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

public interface KeybasedRouting {

	public void create() throws IOException;
	public Future<Void> join(URI bootstrap);
	public Future<List<Node>> findNodes(Key k, int n);
	public Set<Node> getNeighbors();
	
	public KeyFactory getKeyFactory();
	public Node getLocalNode();
	
	public void register(String pattern, NodeConnectionListener listener);
	
	
	public void shutdown();
}

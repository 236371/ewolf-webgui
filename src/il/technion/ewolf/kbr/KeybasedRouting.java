package il.technion.ewolf.kbr;

import il.technion.ewolf.kbr.concurrent.CompletionHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public interface KeybasedRouting {

	
	public void create() throws IOException;
	public void join(Collection<URI> bootstraps) throws Exception;
	public List<Node> findNode(Key k, int n) throws Exception;
	public List<Node> findNode(Key k) throws Exception;
	
	public void register(String tag, MessageHandler handler);
	
	public void sendMessage(Node to, String tag, byte[] msg) throws IOException;
	public Future<byte[]> sendRequest(Node to, String tag, byte[] msg) throws Exception;
	public <A> void sendRequest(Node to, String tag, byte[] msg, A attachment, CompletionHandler<byte[], A> handler);
	
	public KeyFactory getKeyFactory();
	public List<Node> getNeighbours();
	public Node getLocalNode();
}

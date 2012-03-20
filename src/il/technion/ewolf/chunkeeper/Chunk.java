package il.technion.ewolf.chunkeeper;

import il.technion.ewolf.chunkeeper.net.ChunkeeperSerializer;
import il.technion.ewolf.http.HttpConnector;
import il.technion.ewolf.kbr.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Represent a data item that can be download lazily
 * 
 * @author eyal.kibbar@gmail.com
 *
 */
public class Chunk {

	// state
	private ChunkId chunkId = null;
	private Set<Node> srcs = new HashSet<Node>();
	
	// cached
	private byte[] data = null;
	
	// dependencies
	private final HttpConnector connector;
	private final String[] hashAlgos;
	private final String getHandlerPath;
	private final ChunkeeperSerializer serializer;
	
	@Inject
	Chunk(HttpConnector connector,
		@Named("chunkeeper.store.hashalgos") String[] hashAlgos,
		@Named("chunkeeper.handlers.get.path") String getHandlerPath,
		ChunkeeperSerializer serializer) {
		
		this.connector = connector;
		this.hashAlgos = hashAlgos;
		this.getHandlerPath = getHandlerPath;
		this.serializer = serializer;
	}
	
	/**
	 * Adds another chunk location (ip and port) for downloading this
	 * chunk from. Note that the first time addLocator is invoked sets
	 * this chunk identity
	 * 
	 * @param l the chunk locator
	 * @return true if the locator fits (has the same identity as this chunk)
	 */
	public boolean addLocator(ChunkLocator l) {
		if (chunkId == null) {
			// first addLocator sets the chunk id of this chunk
			srcs.add(l.getHolder());
			chunkId = l.getChunkId();
			return true;
		}
		
		if (!chunkId.equals(l.getChunkId()))
			return false;
		
		srcs.add(l.getHolder());
		
		return true;
	}
	
	/**
	 * 
	 * @return the chunk identity. Can be null if chunk has no identity yet
	 */
	public ChunkId getChunkId() {
		return chunkId;
	}
	
	/**
	 * Manually sets the data. This will prevent opening connection for download the data.
	 * This method is very useful for returning cached results
	 * @param data the chunk's data
	 * @return this for fluent interface
	 */
	public Chunk setData(byte[] data) {
		this.data = data;
		return this;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (chunkId == null)
			throw new IllegalStateException("must add at least 1 ChunkLocator b4 using");
		if (obj == null || !getClass().equals(obj.getClass()))
			return false;
		
		Chunk other = (Chunk)obj;
		
		return getChunkId().equals(other.getChunkId());
	}
	
	@Override
	public int hashCode() {
		if (chunkId == null)
			throw new IllegalStateException("must add at least 1 ChunkLocator b4 using");
		return chunkId.hashCode();
	}
	

	/**
	 * Download the data and from the remote node and deserialize it
	 * @return the deserialized data
	 * @throws IOException if the data was not successfully downloaded
	 * @throws ClassNotFoundException could not deserialize the data
	 */
	public Serializable download() throws IOException, ClassNotFoundException {
		byte[] raw = downloadRaw();
		
		InputStream in = null;
		ObjectInputStream oin = null;
		try {
			in = new ByteArrayInputStream(raw);
			oin = new ObjectInputStream(in);
			return (Serializable) oin.readObject();
		} finally {
			try { oin.close(); } catch (Exception e) {}
			try { in.close(); } catch (Exception e) {}
		}
	}
	
	
	/**
	 * Download the data from the remote node.
	 * Note that this method caches the download data, thus only the first invocation
	 * will actually open a connection with the remote nodes for downloading.
	 * @return the raw data in its byte array format
	 * @throws IOException open connection with all data's sources failed
	 */
	public byte[] downloadRaw() throws IOException {
		if (data != null)
			return data;
		
		// TODO: sort nodes according to policy/reasonable order, like putting the local
		// node first
		
		String query = "chunkid="+serializer.toUrlSafeString(getChunkId());
		HttpRequest req = new BasicHttpRequest("GET", getHandlerPath+"?"+query);
		
		for (Node n : srcs) {
			try {
				
				HttpResponse res = connector.send(n, req);
				data = EntityUtils.toByteArray(res.getEntity());
				
				// validate data
				if (!new ChunkId(chunkId.getKey(), data, hashAlgos).equals(chunkId)) {
					data = null;
					continue;
				}
				return data;
				
			} catch (Exception e) {
				// nothing to do, continue to the next node
				e.printStackTrace();
			}
		}
		throw new IOException("could not find any suitable node to download");
	}
	
}

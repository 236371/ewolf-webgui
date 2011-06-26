package dht;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.Future;

public interface DHT {

	public void create();
	public Key getNodeID();
	public void join(URI bootstrap) throws IOException;
	public void shutdown();
	
	public Future<Void> put(Key key, Serializable value) throws IOException;
	public Future<Void> put(Key key, Object value, SerializerFactory serializer) throws IOException;
	
	public Set<Object> get(Key key) throws IOException;
	public Set<Object> get(Key key, SerializerFactory serializer) throws IOException;
	
	public KeyFactory getKeyFactory();
	
	public Set<Object> localGet(Key key) throws ClassNotFoundException, IOException;
	public Set<Object> localGet(Key key, SerializerFactory serializer) throws ClassNotFoundException, IOException;
}

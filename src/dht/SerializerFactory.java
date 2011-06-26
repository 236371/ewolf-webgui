package dht;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;

public interface SerializerFactory {

	public ObjectInput createObjectInput(InputStream is) throws IOException;
	public ObjectOutput createObjectOutput(OutputStream os) throws IOException;
	
}

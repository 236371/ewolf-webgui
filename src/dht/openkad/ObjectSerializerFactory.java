package dht.openkad;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import dht.SerializerFactory;

class ObjectSerializerFactory implements SerializerFactory {

	@Override
	public ObjectInput createObjectInput(InputStream is) throws IOException {
		return new ObjectInputStream(is);
	}

	@Override
	public ObjectOutput createObjectOutput(OutputStream os) throws IOException {
		return new ObjectOutputStream(os);
	}

}

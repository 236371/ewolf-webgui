package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.msg.KadMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class ObjectKadSerializer implements KadSerializer {

	@Override
	public KadMessage read(InputStream in) throws IOException, ClassCastException, ClassNotFoundException {
		ObjectInputStream oin = null;
		try {
			oin = new ObjectInputStream(in);
			return (KadMessage)oin.readObject();
		} finally {
			try { oin.close(); } catch (Exception e) {}
		}
	}

	@Override
	public void write(KadMessage msg, OutputStream out) throws IOException {
		ObjectOutputStream oout = null;
		try {
			oout = new ObjectOutputStream(out);
			oout.writeObject(msg);
		} finally {
			oout.close();
		}
		
	}

}

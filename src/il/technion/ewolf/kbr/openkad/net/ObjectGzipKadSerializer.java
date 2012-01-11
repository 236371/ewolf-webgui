package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.msg.KadMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ObjectGzipKadSerializer implements KadSerializer {

	@Override
	public KadMessage read(InputStream in) throws IOException, ClassCastException, ClassNotFoundException {
		ObjectInputStream oin = null;
		GZIPInputStream din = null;
		try {
			din = new GZIPInputStream(in);
			oin = new ObjectInputStream(din);
			return (KadMessage)oin.readObject();
		} finally {
			try { oin.close(); } catch (Exception e) {}
			try { din.close(); } catch (Exception e) {}
		}
	}

	@Override
	public void write(KadMessage msg, OutputStream out) throws IOException {
		ObjectOutputStream oout = null;
		GZIPOutputStream dout = null;
		try {
			dout = new GZIPOutputStream(out);
			oout = new ObjectOutputStream(dout);
			oout.writeObject(msg);
		} finally {
			oout.close();
			dout.close();
		}
		
	}

}

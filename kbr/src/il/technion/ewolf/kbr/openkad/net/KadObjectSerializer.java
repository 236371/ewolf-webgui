package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.KadMessage;
import il.technion.ewolf.kbr.openkad.KadMessageBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;


class KadObjectSerializer implements KadMessageSerializer {


	@Override
	public void writeKadMessage(KadMessage msg, OutputStream out) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(msg);
		oout.flush();
	}

	@Override
	public KadMessage readKadMessage(InetAddress src, InputStream in)
			throws IOException, ClassNotFoundException {
		ObjectInputStream oin = new ObjectInputStream(in);
		return new KadMessageBuilder((KadMessage) oin.readObject())
			.fixLastHopAddress(src)
			.build();
	}

}

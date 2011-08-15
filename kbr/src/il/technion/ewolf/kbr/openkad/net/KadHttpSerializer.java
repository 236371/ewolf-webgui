package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.KadMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;


class KadHttpSerializer implements KadMessageSerializer {


	@Override
	public void writeKadMessage(KadMessage msg, OutputStream out) throws IOException {
		//HttpRequest req;
		
	}

	@Override
	public KadMessage readKadMessage(InetAddress src, InputStream in)
			throws IOException, ClassNotFoundException {
		return null;
		/*
		
		//ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bi.toByteArray()));
		return new KadMessageBuilder((KadMessage) oin.readObject())
			.fixLastHopAddress(src)
			.build();
		*/
	}

}

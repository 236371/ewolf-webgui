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
		//ByteArrayOutputStream bo = new ByteArrayOutputStream();
		//ObjectOutputStream oout = new ObjectOutputStream(bo);
		ObjectOutputStream oout = new ObjectOutputStream(out);
		oout.writeObject(msg);
		oout.flush();
		//System.out.println(bo.toByteArray().length);
		//out.write(bo.toByteArray());
		//out.flush();
	}

	@Override
	public KadMessage readKadMessage(InetAddress src, InputStream in)
			throws IOException, ClassNotFoundException {
		/*
		ByteArrayOutputStream bi = new ByteArrayOutputStream();
		byte[] buff = new byte[4096];
		int n=0;
		n = in.read(buff);
		bi.write(buff, 0, n);
		
		System.err.println(bi.toByteArray().length);
		*/
		ObjectInputStream oin = new ObjectInputStream(in);
		//ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bi.toByteArray()));
		return new KadMessageBuilder((KadMessage) oin.readObject())
			.fixLastHopAddress(src)
			.build();
	}

}

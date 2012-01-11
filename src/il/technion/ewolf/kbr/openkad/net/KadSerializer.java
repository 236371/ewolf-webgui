package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.msg.KadMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface KadSerializer {

	KadMessage read(InputStream in) throws IOException, ClassCastException, ClassNotFoundException;
	
	void write(KadMessage msg, OutputStream out) throws IOException;
	
}

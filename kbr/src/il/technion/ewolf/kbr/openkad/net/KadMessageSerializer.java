package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.KadMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;


public interface KadMessageSerializer {
	
	
	KadMessage readKadMessage(InetAddress src, InputStream in) throws IOException, ClassNotFoundException;
	
	void writeKadMessage(KadMessage msg, OutputStream out) throws IOException;
	
}

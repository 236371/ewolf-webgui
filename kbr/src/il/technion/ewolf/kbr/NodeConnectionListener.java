package il.technion.ewolf.kbr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public interface NodeConnectionListener {

	public void onIncomingMessage(String tag, Node from, InputStream in) throws IOException;
	public void onIncomingMessage(String tag, Node from, InputStream in, OutputStream out) throws IOException;
	public void onIncomingConnection(String tag, Node from, Socket sock) throws IOException;
}

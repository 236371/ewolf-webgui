package il.technion.ewolf.kbr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Default empty implementation of the NodeConnectionListener interface
 * @author eyal
 * @see NodeConnectionListener
 */
abstract public class DefaultNodeConnectionListener implements NodeConnectionListener {

	@Override
	public void onIncomingMessage(String tag, Node from, InputStream in) throws IOException {}
	
	@Override
	public void onIncomingConnection(String tag, Node from, Socket sock) throws IOException {}
	
	@Override
	public void onIncomingMessage(String tag, Node from, InputStream in, OutputStream out) throws IOException {}
}

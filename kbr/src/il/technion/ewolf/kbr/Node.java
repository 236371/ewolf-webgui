package il.technion.ewolf.kbr;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.concurrent.Future;

public interface Node extends KeyHolder {
	public Future<Socket> openConnection(String tag) throws IOException;
	public Future<DatagramSocket> openUdpConnection(String tag) throws IOException;
	public OutputStream sendMessage(String tag) throws IOException;
	
}

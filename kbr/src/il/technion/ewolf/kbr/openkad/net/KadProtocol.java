package il.technion.ewolf.kbr.openkad.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public enum KadProtocol {

	otcpkad {
		@Override
		public KadMessageSerializer getSerializer() {
			return kadObjectSerializer;
		}
		
		@Override
		public KadConnection openConnection(InetSocketAddress addr) throws IOException {
			return openTcpConnection(this, addr);
		}
		
		@Override
		public SelectableChannel openChannel(InetSocketAddress addr) throws IOException {
			return openTcpChannel(addr);
		}
		
		@Override
		public boolean canKeepAlive() {
			return true;
		}
	},
	
	oudpkad {

		@Override
		public KadMessageSerializer getSerializer() {
			return kadObjectSerializer;
		}
		
		@Override
		public KadConnection openConnection(InetSocketAddress addr) throws IOException {
			return openUdpConnection(this, addr);
		}
		
		@Override
		public SelectableChannel openChannel(InetSocketAddress addr) throws IOException {
			return openUdpChannel(addr);
		}
		
		@Override
		public boolean canKeepAlive() {
			return false;
		}
	},
	
	http {

		@Override
		public KadMessageSerializer getSerializer() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public KadConnection openConnection(InetSocketAddress addr) throws IOException {
			return openTcpConnection(this, addr);
		}

		@Override
		public SelectableChannel openChannel(InetSocketAddress addr) throws IOException {
			return openTcpChannel(addr);
		}

		@Override
		public boolean canKeepAlive() {
			return true;
		}
		
	};
	
	private static final KadMessageSerializer kadObjectSerializer = new KadObjectSerializer();
	
	public abstract KadMessageSerializer getSerializer();
	public abstract KadConnection openConnection(InetSocketAddress addr) throws IOException;
	public abstract SelectableChannel openChannel(InetSocketAddress addr) throws IOException;
	public abstract boolean canKeepAlive();
	
	private static KadConnection openTcpConnection(KadProtocol protocol, InetSocketAddress addr) throws IOException {
		SocketChannel chan = SocketChannel.open();
		chan.configureBlocking(false);
		return new KadConnection(chan, protocol, addr);
	}
	
	private static KadConnection openUdpConnection(KadProtocol protocol, InetSocketAddress addr) throws IOException {
		DatagramChannel chan = DatagramChannel.open();
		chan.configureBlocking(false);
		return new KadConnection(chan, protocol, addr);
	}
	
	private static SelectableChannel openTcpChannel(InetSocketAddress addr) throws IOException {
		ServerSocketChannel chan = ServerSocketChannel.open();
		chan.configureBlocking(false);
		chan.socket().bind(addr);
		return chan;
	}
	
	private static SelectableChannel openUdpChannel(InetSocketAddress addr) throws IOException {
		DatagramChannel chan = DatagramChannel.open();
		chan.configureBlocking(false);
		chan.socket().bind(addr);
		return chan;
	}
}

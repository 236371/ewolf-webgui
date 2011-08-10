package il.technion.ewolf.kbr.openkad.net;

import il.technion.ewolf.kbr.openkad.KadMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;


public class KadConnection {

	// these tuning params, should not effect the correctness
	private final static int timeout = 1000;
	private final static int buffSize = 4096;
	
	private final KadProtocol protocol;
	private final SelectableChannel chan;
	private final InetSocketAddress remoteAddr;
	private final AtomicBoolean connected = new AtomicBoolean(false);
	
	//public static AtomicInteger cnt = new AtomicInteger(0);
	
	KadConnection(SelectableChannel chan, KadProtocol protocol, InetSocketAddress remoteAddr) {
		this.protocol = protocol;
		this.chan = chan;
		this.remoteAddr = remoteAddr;
		if (!(chan instanceof SocketChannel) && !(chan instanceof DatagramChannel))
			throw new UnsupportedOperationException();
		//cnt.incrementAndGet();
	}
	
	KadConnection(SelectableChannel chan, KadProtocol protocol) {
		this.protocol = protocol;
		this.chan = chan;
		connected.set(true);
		
		if (chan instanceof SocketChannel) {
			
			if (!((SocketChannel)chan).isConnected())
				throw new NotYetConnectedException();
			this.remoteAddr = (InetSocketAddress)((SocketChannel)chan).socket().getRemoteSocketAddress();
			
		} else if (chan instanceof DatagramChannel) {
			
			if (!((DatagramChannel)chan).isConnected())
				throw new NotYetConnectedException();
			this.remoteAddr = (InetSocketAddress)((DatagramChannel)chan).socket().getRemoteSocketAddress();
			
		} else
			throw new UnsupportedOperationException();
		//cnt.incrementAndGet();
	}
	
	public boolean isConnected() {
		return connected.get();
	}
	
	public synchronized void connect() throws IOException {
		if (connected.get())
			return;
		
		if (chan instanceof SocketChannel) {
			((SocketChannel)chan).connect(remoteAddr);
			Selector selector = Selector.open();
			SelectionKey key = chan.register(selector, SelectionKey.OP_CONNECT);
			// block until connected
			try {
				int s = selector.select(timeout);
				if (s != 1) {
					throw new SocketTimeoutException(""+s);
				}
				if (!((SocketChannel)chan).finishConnect())
					throw new IOException("connection error");
			} finally {
				key.cancel();
				selector.close();
			}
		}
		else if (chan instanceof DatagramChannel) {
			((DatagramChannel)chan).connect(remoteAddr);
		}
		connected.set(true);
	}
	
	public synchronized void sendMessage(KadMessage msg) throws IOException {
		connect();
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		protocol.getSerializer().writeKadMessage(msg, bytes);
		bytes.close();
		ByteBuffer buff = ByteBuffer.wrap(bytes.toByteArray());
		
		while (buff.hasRemaining()) {
			if ((chan.validOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
				Selector selector = Selector.open();
				SelectionKey key = chan.register(selector, SelectionKey.OP_WRITE);
				try {
					int s = selector.select(timeout);
					if (s != 1)
						throw new SocketTimeoutException(""+s);
				} finally {
					key.cancel();
					selector.close();
				}
			}
			
			if (chan instanceof SocketChannel) {
				
				//int n = ((SocketChannel)chan).write(buff);
				((SocketChannel)chan).write(buff);
				//System.out.print("sending "+n+ " to "+((SocketChannel)chan).socket().getRemoteSocketAddress());
				//System.out.println(" from "+((SocketChannel)chan).socket().getLocalSocketAddress());
			} else if (chan instanceof DatagramChannel) {
				((DatagramChannel)chan).write(buff);
			} else {
				throw new AssertionError("invalid channel");
			}
		}
	}
	
	public synchronized KadMessage recvMessage() throws IOException {
		
		//System.out.print("recv from "+((SocketChannel)chan).socket().getRemoteSocketAddress());
		//System.out.println(" to "+((SocketChannel)chan).socket().getLocalSocketAddress());
		
		InputStream in = new InputStream() {
			
			private final ByteBuffer buff = ByteBuffer.allocate(buffSize);
			private int buffOff=0;
			
			@Override
			public int read() throws IOException {
				//System.out.println("read()");
				byte[] b = new byte[1];
				return read(b) == -1 ? -1 : (int)b[0];
			}
			
			@Override
			public int read(byte[] b) throws IOException {
				//System.out.println("read(b)");
				return read(b, 0, b.length);
			}
			
			private int fillLocalBuffer() throws IOException {
				buff.rewind();
				buffOff = 0;
				
				if ((chan.validOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
					Selector selector = Selector.open();
					SelectionKey key = chan.register(selector, SelectionKey.OP_READ);
					try {
						int n = selector.select(timeout); 
						if (n != 1) {
							//System.err.println("im here n = " +n);
							return -1;
						}
					} finally {
						key.cancel();
						selector.close();
					}
				}
				
				if (chan instanceof SocketChannel) {
					int n = ((SocketChannel)chan).read(buff);
					//System.out.println("n = "+n);
					return n;
				} else if (chan instanceof DatagramChannel) {
					return ((DatagramChannel)chan).read(buff);
				}
				return -1;
			}
			
			private int fillUserBuffer(byte[] b, int off, int len) {
				//System.out.println("buffOff = "+buffOff+" off = "+off+" len = "+len);
				len = Math.min(len, buff.position() - buffOff);
				/*
				for (int i = buffOff; i < len; ++i) {
					b[off+i] = buff.get(buffOff+i);
				}
				*/
				System.arraycopy(buff.array(), buffOff, b, off, len);
				buffOff += len;
				//System.out.println("buffOff = "+buffOff+" off = "+off+" len = "+len);
				return len;
			}
			
			@Override
			public synchronized int read(byte[] b, int off, int len) throws IOException {
				if (buff.position() == buffOff) {
					if (fillLocalBuffer() == -1) {
						return -1;
					}
				}
				return fillUserBuffer(b, off, len);
			}
		};
		
		
		try {
			
			return protocol.getSerializer().readKadMessage(remoteAddr.getAddress(), in);
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	
	public InetSocketAddress getRemoteSocketAddress() {
		return remoteAddr;
	}
	
	public SelectableChannel getChannel() {
		return chan;
	}
	
	public void close() {
		try {
			chan.close();
			//cnt.decrementAndGet();
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	public KadProtocol getProtocol() {
		return protocol;
	}
	
	public String toString() {
		return protocol.name()+"://"+remoteAddr.getAddress().getHostName()+":"+remoteAddr.getPort()+"/";
	}
}

package dht.openkad;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.google.inject.Inject;
import com.google.inject.name.Named;

class KadConnectionDispacher implements KadConnectionListener {

	private KadConnectionListener connListener = null;
	private final ExecutorService executor;
	
	@Inject
	KadConnectionDispacher(@Named("kad.incomming.threadpool") ExecutorService executor) {
		this.executor = executor;
	}
	
	
	void setConnListener(KadConnectionListener connListener) {
		this.connListener = connListener;
	}


	@Override
	public void onIncomingConnection(final KadConnection conn) throws IOException {
		
		executor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					if (connListener != null)
						connListener.onIncomingConnection(conn);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	

}

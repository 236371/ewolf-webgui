package il.technion.ewolf.kbr.openkad.ops;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

public abstract class KadOperation<T> implements Callable<T> {
	
	final Logger logger;
	
	KadOperation(Logger logger) {
		this.logger = logger;
	}

}

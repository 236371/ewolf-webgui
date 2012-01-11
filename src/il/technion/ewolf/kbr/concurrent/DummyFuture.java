package il.technion.ewolf.kbr.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DummyFuture<T> implements Future<T> {

	private final T result;
	private final Throwable exc;
	
	public DummyFuture(T result) {
		this.result = result;
		this.exc = null;
	}
	
	public DummyFuture(Throwable exc) {
		this.result = null;
		this.exc = exc;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return true;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (exc != null)
			throw new ExecutionException(exc);
		return result;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (exc != null)
			throw new ExecutionException(exc);
		return result;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

}

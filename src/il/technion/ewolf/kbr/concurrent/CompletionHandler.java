package il.technion.ewolf.kbr.concurrent;



public interface CompletionHandler<R, A> {
	
	void completed(R msg, A attachment);
	void failed(Throwable exc, A attachment);

}

package il.technion.ewolf.exceptions;

public class ForgeryException extends Exception {

	private static final long serialVersionUID = -2199206167740776297L;

	public ForgeryException() {
		super();
	}

	public ForgeryException(String message, Throwable cause) {
		super(message, cause);
	}

	public ForgeryException(String message) {
		super(message);
	}

	public ForgeryException(Throwable cause) {
		super(cause);
	}

	
}

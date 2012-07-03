package il.technion.ewolf.server.exceptions;

public abstract class HandlerException extends Exception {

	private static final long serialVersionUID = -3912567349985514458L;

	public HandlerException() {
		// TODO Auto-generated constructor stub
	}

	public HandlerException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public HandlerException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public HandlerException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}

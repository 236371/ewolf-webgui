package il.technion.ewolf.server.exceptions;

public class BadRequestException extends HandlerException {

	private static final long serialVersionUID = -5449522980327155975L;

	public BadRequestException() {
		// TODO Auto-generated constructor stub
	}

	public BadRequestException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public BadRequestException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	public BadRequestException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}

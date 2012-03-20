package il.technion.ewolf.exceptions;

public class WallNotFound extends Exception {

	private static final long serialVersionUID = 1633878064103918710L;

	public WallNotFound() {
	}

	public WallNotFound(String message) {
		super(message);
	}

	public WallNotFound(Throwable cause) {
		super(cause);
	}

	public WallNotFound(String message, Throwable cause) {
		super(message, cause);
	}

}

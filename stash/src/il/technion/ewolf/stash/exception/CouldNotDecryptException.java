package il.technion.ewolf.stash.exception;

public class CouldNotDecryptException extends Exception {

	private static final long serialVersionUID = 6739045827278274904L;

	public CouldNotDecryptException() {
		super();
	}

	public CouldNotDecryptException(String message, Throwable cause) {
		super(message, cause);
	}

	public CouldNotDecryptException(String message) {
		super(message);
	}

	public CouldNotDecryptException(Throwable cause) {
		super(cause);
	}
	
	

}

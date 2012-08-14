package il.technion.ewolf.server;

public class EWolfResponse {
	private String result;
	private String errorMessage;

	public static final String RES_SUCCESS = "SUCCESS";
	public static final String RES_NOT_FOUND = "ITEM_NOT_FOUND";
	public static final String RES_BAD_REQUEST = "BAD_REQUEST";
	public static final String RES_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
	public static final String RES_GENERIC_ERROR = "GENERAL_ERROR";
	public static final String RES_UNAVAILBLE_REQUEST = "UNAVAILBLE_REQUEST";

	public EWolfResponse(String result) {
		this.result = result;
	}

	public EWolfResponse() {
		this.result = RES_SUCCESS;
	}

	public EWolfResponse(String result, String errorMessage) {
		this.result = result;
		this.errorMessage = errorMessage;
	}

	public String getResult() {
		return result;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
}

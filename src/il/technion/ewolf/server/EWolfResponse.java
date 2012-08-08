package il.technion.ewolf.server;

public class EWolfResponse {
	String result;
	public static final String RES_SUCCESS = "success";
	public static final String RES_NOT_FOUND = "not found";
	public static final String RES_BAD_REQUEST = "bad request";
	public static final String RES_INTERNAL_SERVER_ERROR = "internal server error";
	public static final String RES_GENERIC_ERROR = "error";

	public EWolfResponse(String result) {
		this.result = result;
	}
	
	public EWolfResponse() {
		this.result = RES_SUCCESS;
	}
	
	public String result() {
		return result;
	}
}

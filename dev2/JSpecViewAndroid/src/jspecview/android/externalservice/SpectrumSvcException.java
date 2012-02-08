package jspecview.android.externalservice;

public class SpectrumSvcException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7819879449162850943L;

	public SpectrumSvcException(){
		super();
	}
	
	public SpectrumSvcException(String message){
		super(message);
	}
	
	public SpectrumSvcException(Throwable innerException){
		super(innerException);
	}	
	
	public SpectrumSvcException(String message, Throwable innerException){
		super(message, innerException);
	}
}

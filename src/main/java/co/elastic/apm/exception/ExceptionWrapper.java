package co.elastic.apm.exception;

public class ExceptionWrapper extends Exception {
    private static final long serialVersionUID = 1L;

    public ExceptionWrapper(Throwable cause, String action) {
        super(getMessage(cause, action), cause);
    }
    
    public ExceptionWrapper(Throwable cause, String action, String url) {
        super(getMessage(cause, action) + ". Url: " + url, cause);
    }
    
    private static String getMessage(Throwable cause, String action) {
        if (action != null) {
            return cause.getMessage() + ". action=" + action;
        }
        return cause.getMessage();
    }
}

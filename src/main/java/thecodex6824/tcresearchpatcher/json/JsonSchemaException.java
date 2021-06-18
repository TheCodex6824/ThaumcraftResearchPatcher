package thecodex6824.tcresearchpatcher.json;

/**
 * Exception class for instances where the provided JSON does match the expected format.
 * @author TheCodex6824
 */
public class JsonSchemaException extends RuntimeException {

    private static final long serialVersionUID = -202697203598380455L;

    public JsonSchemaException(String cause) {
        super(cause);
    }
    
    public JsonSchemaException(Throwable cause) {
        super(cause);
    }
    
    public JsonSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
    
}

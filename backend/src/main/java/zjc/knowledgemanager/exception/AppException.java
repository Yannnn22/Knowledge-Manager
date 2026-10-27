package zjc.knowledgemanager.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AppException extends RuntimeException {

    private final static Logger logger = LoggerFactory.getLogger(AppException.class);

    public AppException(String msg) {
        super(msg);
        logger.error("Spring AI Playground app exception: {}", msg);
    }

}
package cx.ath.mancel01.modules.exception;

public class MissingDependenciesException extends RuntimeException {

    public MissingDependenciesException() {}

    public MissingDependenciesException(String msg) {
        super(msg);
    }
    
    public MissingDependenciesException(Throwable t) {
        super(t);
    }
}

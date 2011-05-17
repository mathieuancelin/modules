package cx.ath.mancel01.modules.exception;

public class CircularDependencyException extends RuntimeException {

    public CircularDependencyException() {
    }

    public CircularDependencyException(String msg) {
        super(msg);
    }
}

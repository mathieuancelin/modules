package cx.ath.mancel01.modules.exception;

public class ModuleNotReadableException extends RuntimeException {

    public ModuleNotReadableException() {
    }

    public ModuleNotReadableException(String msg) {
        super(msg);
    }

    public ModuleNotReadableException(String msg, Throwable t) {
        super(msg, t);
    }
}

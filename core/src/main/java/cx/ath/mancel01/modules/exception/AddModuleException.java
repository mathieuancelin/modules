package cx.ath.mancel01.modules.exception;

public class AddModuleException extends RuntimeException {

    public AddModuleException() {
    }

    public AddModuleException(String msg) {
        super(msg);
    }

    public AddModuleException(Throwable t) {
        super(t);
    }
}

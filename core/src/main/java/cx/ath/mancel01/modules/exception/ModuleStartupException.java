package cx.ath.mancel01.modules.exception;

public class ModuleStartupException extends RuntimeException {

    public ModuleStartupException() {}

    public ModuleStartupException(String msg) {
        super(msg);
    }

    public ModuleStartupException(Throwable t) {
        super(t);
    }
}

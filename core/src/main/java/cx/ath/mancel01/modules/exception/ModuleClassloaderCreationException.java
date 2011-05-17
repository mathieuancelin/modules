package cx.ath.mancel01.modules.exception;

public class ModuleClassloaderCreationException extends RuntimeException {

    public ModuleClassloaderCreationException() {}

    public ModuleClassloaderCreationException(String msg) {
        super(msg);
    }

    public ModuleClassloaderCreationException(Throwable t) {
        super(t);
    }
}

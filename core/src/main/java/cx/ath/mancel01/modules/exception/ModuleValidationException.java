package cx.ath.mancel01.modules.exception;

public class ModuleValidationException extends RuntimeException {

    public ModuleValidationException() {
    }

    public ModuleValidationException(String msg) {
        super(msg);
    }
}

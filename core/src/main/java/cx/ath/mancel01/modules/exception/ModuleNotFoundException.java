package cx.ath.mancel01.modules.exception;

public class ModuleNotFoundException extends RuntimeException {

    public ModuleNotFoundException() {
    }

    public ModuleNotFoundException(String msg) {
        super(msg);
    }
}

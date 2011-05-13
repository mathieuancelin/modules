package cx.ath.mancel01.modules.util;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(MainThread.class);

    private final Method main;

    private AtomicBoolean ended = new AtomicBoolean(false);

    public MainThread(Method main) {
        this.main = main;
    }

    @Override
    public void run() {
        try {
            Object arg = new String[] {};
            main.invoke(null, arg);
        } catch (Exception ex) {
            logger.error("Error while running main method : ", ex);
        }
        ended.compareAndSet(false, true);
    }

    public boolean isEnded() {
        return ended.get();
    }
}

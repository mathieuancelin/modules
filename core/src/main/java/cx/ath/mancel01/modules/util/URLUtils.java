package cx.ath.mancel01.modules.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class URLUtils {

    public static URL url(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static URL url(String file) {
        try {
            return new File(file).toURI().toURL();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}

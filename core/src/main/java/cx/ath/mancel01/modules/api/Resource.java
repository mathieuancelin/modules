package cx.ath.mancel01.modules.api;

import java.io.InputStream;
import java.net.URL;

public interface Resource {

    URL getURL();

    InputStream getInsputStream();

    String getName();
}

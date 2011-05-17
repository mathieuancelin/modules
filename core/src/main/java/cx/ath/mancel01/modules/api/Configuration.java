package cx.ath.mancel01.modules.api;

import java.net.URL;
import java.util.Collection;

public interface Configuration {

    String name();

    String version();

    String identifier();

    Collection<Dependency> dependencies();

    Collection<Dependency> optionalDependencies();

    boolean startable();

    String mainClass();

    URL rootResource();

}

package cx.ath.mancel01.modules.module;

import cx.ath.mancel01.modules.Modules;
import cx.ath.mancel01.modules.api.Dependency;
import cx.ath.mancel01.modules.exception.CircularDependencyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DependencyImpl implements Dependency {

    private final String identifier;
    private final String name;
    private final String version;

    public DependencyImpl(String identifier) {
        this.identifier = identifier;
        String tmpName = "";
        String tmpVersion = "";
        try {
            tmpName = identifier.split(":")[0];
        } catch (Exception e) {}
        try {
            tmpVersion = identifier.split(":")[1];
        } catch (Exception e) {}
        name = tmpName;
        version = tmpVersion;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    static Collection<Dependency> getDependencies(Collection<String> dependencies) {
        List<Dependency> deps = new ArrayList<Dependency>();
        for (final String dep : dependencies) {
            deps.add(new DependencyImpl(dep));
        }
        return deps;
    }

    static void checkForCircularDependencies(List<String> marked, Module m, Modules modules) {
        if (marked.contains(m.identifier)) {
            // TODO : show refs chain
            throw new CircularDependencyException("Circular reference for module " + m.identifier);
        }
        marked.add(m.identifier);
        for (Dependency dependency : m.dependencies()) {
            Module child = modules.getModules().get(dependency.identifier());
            if (child != null) {
                checkForCircularDependencies(marked, child, modules);
            }
        }
        marked.remove(m.identifier);
    }
}

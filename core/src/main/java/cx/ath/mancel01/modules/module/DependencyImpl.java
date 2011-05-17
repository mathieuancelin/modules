package cx.ath.mancel01.modules.module;

import cx.ath.mancel01.modules.Modules;
import cx.ath.mancel01.modules.api.Dependency;
import cx.ath.mancel01.modules.exception.CircularDependencyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyImpl implements Dependency {

    private static final Map<String, Dependency> existing = new HashMap<String, Dependency>();
    private final String identifier;
    private final String name;
    private final String version;

    public static synchronized Dependency getFromId(final String identifier) {
        if (!existing.containsKey(identifier)) {
            existing.put(identifier, new DependencyImpl(identifier));
        }
        return existing.get(identifier);
    }

    private DependencyImpl(String identifier) {
        this.identifier = identifier;
        String tmpName = identifier;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DependencyImpl other = (DependencyImpl) obj;
        if ((this.identifier == null) ? (other.identifier != null) : !this.identifier.equals(other.identifier)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.identifier != null ? this.identifier.hashCode() : 0);
        return hash;
    }

    public static Collection<Dependency> getDependencies(Collection<String> dependencies) {
        List<Dependency> deps = new ArrayList<Dependency>();
        for (final String dep : dependencies) {
            deps.add(getFromId(dep));
        }
        return deps;
    }

    static void checkForCircularDependencies(List<String> marked, Module m, Modules modules, Set<String> areCircular) {
        if (marked.contains(m.identifier)) {
            if ( Modules.failOnCircularRefs ) {
                StringBuilder builder = new StringBuilder();
                builder.append(m.identifier);
                builder.append(" => ");
                boolean back = true;
                int i = (marked.size() - 1);
                while (back) {
                    try {
                        String dep = marked.get(i);
                        if (dep.equals(m.identifier)) {
                            back = false;
                        }
                        builder.append(dep);
                        if (back != false) {
                            builder.append(" => ");
                        }
                        i--;
                    } catch (Exception e) {
                        back = false;
                    }
                }
                throw new CircularDependencyException("Circular reference for module "
                        + m.identifier + " :\n    Circular refs chain : " + builder.toString() + "\n");
            }
            areCircular.add(m.identifier);
        } else {
            marked.add(m.identifier);
            for (Dependency dependency : m.dependencies()) {
                Module child = modules.getModules().get(dependency);
                if (child != null) {
                    checkForCircularDependencies(marked, child, modules, areCircular);
                }
            }
            marked.remove(m.identifier);
        }
    }
}

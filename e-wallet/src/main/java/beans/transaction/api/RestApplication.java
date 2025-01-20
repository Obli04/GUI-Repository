package beans.transaction.api;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Configures the REST API path and registers resources for the store simulator.
 * @author Arthur PHOMMACHANH - xphomma00
 */
@ApplicationPath("/api")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(StoreApiSimulator.class);
        return classes;
    }
}

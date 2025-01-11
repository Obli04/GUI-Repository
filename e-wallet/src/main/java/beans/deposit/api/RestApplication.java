package beans.deposit.api;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application configuration class.
 * Configures the REST API endpoint path and registers API resources.
 *
 * @author Danilo Spera
 */
@ApplicationPath("/api")
public class RestApplication extends Application {
    
    /**
     * Registers REST API resource classes.
     * This method tells JAX-RS which classes should be treated as REST endpoints.
     *
     * @return Set of REST resource classes
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(BankApiSimulator.class);
        return classes;
    }
} 
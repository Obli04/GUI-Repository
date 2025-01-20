package beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

/**
 * Configures the session timeout for the application.
 * This class sets the maximum inactive interval for user sessions,
 * ensuring that sessions are automatically invalidated after a specified period of inactivity.
 * 
 * @author Davide Scaccia - xscaccd00
 */
@Named
@ApplicationScoped
public class SessionTimeoutConfig {
    
    private static final int SESSION_TIMEOUT_IN_MINUTES = 30;
    
    /**
     * Configures the session timeout for the current user session.
     * This method retrieves the current FacesContext and sets the maximum
     * inactive interval for the associated HttpSession.
     */
    public void configureSessionTimeout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_IN_MINUTES * 60);
    }
}
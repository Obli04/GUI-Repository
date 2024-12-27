package beans;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

@Named
@ApplicationScoped
public class SessionTimeoutConfig {
    
    private static final int SESSION_TIMEOUT_IN_MINUTES = 30;
    
    public void configureSessionTimeout() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(true);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_IN_MINUTES * 60);
    }
}
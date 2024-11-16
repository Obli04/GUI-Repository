import beans.entities.User;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@Named
@RequestScoped
public class UserBean {
    private String email;
    private String password;

    private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("eWalletPU");

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String login() {
        EntityManager em = emf.createEntityManager();
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email AND u.password = :password", User.class)
                          .setParameter("email", email)
                          .setParameter("password", password)
                          .getSingleResult();
            return "dashboard.xhtml?faces-redirect=true";
        } catch (Exception e) {
            return "login.xhtml?faces-redirect=true";
        } finally {
            em.close();
        }
    }

    public String register() {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            User user = new User();
            user.setEmail(email);
            user.setPassword(password);
            em.persist(user);
            em.getTransaction().commit();
            return "login.xhtml?faces-redirect=true";
        } catch (Exception e) {
            em.getTransaction().rollback();
            return "register.xhtml?faces-redirect=true";
        } finally {
            em.close();
        }
    }
}

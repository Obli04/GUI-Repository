package beans;

import beans.entities.Friends;
import beans.entities.User;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Bean for managing friendships.
 * Provides functionality to retrieve, add, and remove friends.
 *
 * @author Arthur
 */
@Named
@SessionScoped
public class FriendBean implements Serializable {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private UserBean userBean; // Ensure UserBean is properly injected and accessible

    private User friendToAdd;

    /**
     * Retrieves the list of friends for the currently logged-in user.
     */
    public List<Friends> getFriends() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        return em.createQuery("SELECT f FROM Friends f WHERE f.user1 = :user OR f.user2 = :user", Friends.class)
                .setParameter("user", currentUser)
                .getResultList();
    }

    /**
     * Checks if a given user is already a friend of the currently logged-in user.
     */
    public boolean isFriend(User potentialFriend) {
        User currentUser = userBean.getCurrentUser();
        if (currentUser == null || potentialFriend == null) {
            return false;
        }

        Long count = em.createQuery(
                        "SELECT COUNT(f) FROM Friends f WHERE (f.user1 = :user1 AND f.user2 = :user2) OR (f.user1 = :user2 AND f.user2 = :user1)", Long.class)
                .setParameter("user1", currentUser)
                .setParameter("user2", potentialFriend)
                .getSingleResult();

        return count > 0;
    }

    /**
     * Adds a new friend to the currently logged-in user's friend list.
     */
    @Transactional
    public void addFriend(User friend) {
        User currentUser = userBean.getCurrentUser();

        if (currentUser == null || friend == null) {
            throw new IllegalArgumentException("Both users must be valid.");
        }

        if (isFriend(friend)) {
            throw new IllegalStateException("You are already friends with this user.");
        }

        Friends newFriendship = new Friends();
        newFriendship.setUser1(currentUser);
        newFriendship.setUser2(friend);

        em.persist(newFriendship);
    }

    /**
     * Removes an existing friend from the currently logged-in user's friend list.
     */
    @Transactional
    public void removeFriend(User friend) {
        User currentUser = userBean.getCurrentUser();

        if (currentUser == null || friend == null) {
            throw new IllegalArgumentException("Both users must be valid.");
        }

        // Fetch the friendship relationship
        List<Friends> friendships = em.createQuery(
                        "SELECT f FROM Friends f WHERE (f.user1 = :user1 AND f.user2 = :user2) OR (f.user1 = :user2 AND f.user2 = :user1)", Friends.class)
                .setParameter("user1", currentUser)
                .setParameter("user2", friend)
                .getResultList();

        // Remove each friendship found (in case of duplicates)
        for (Friends friendship : friendships) {
            em.remove(friendship);
        }

        // Force a refresh of the lists
        em.flush();
    }


    /**
     * Retrieves a list of users who are not yet friends with the currently logged-in user.
     */
    public List<User> getPotentialFriends() {
        User currentUser = userBean.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        // Fetch users who are not in any friendship relationship with the current user
        String jpql = "SELECT u FROM User u " +
                "WHERE u != :currentUser AND u.id NOT IN " +
                "(SELECT f.user1.id FROM Friends f WHERE f.user2 = :currentUser) AND u.id NOT IN " +
                "(SELECT f.user2.id FROM Friends f WHERE f.user1 = :currentUser)";

        return em.createQuery(jpql, User.class)
                .setParameter("currentUser", currentUser)
                .getResultList();
    }


    /**
     * Getter for userBean to ensure it is accessible in XHTML.
     */
    public UserBean getUserBean() {
        return userBean;
    }

    /**
     * Gets the current friend to add.
     */
    public User getFriendToAdd() {
        return friendToAdd;
    }

    /**
     * Sets the friend to add.
     */
    public void setFriendToAdd(User friendToAdd) {
        this.friendToAdd = friendToAdd;
    }
}

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
 * @author Arthur PHOMMACHANH - xphomma00
 */
@Named
@SessionScoped
public class FriendBean implements Serializable {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private UserBean userBean;

    private User friendToAdd;
    private String searchEmail;
    private List<User> searchResults;

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

        List<Friends> friendships = em.createQuery(
                        "SELECT f FROM Friends f WHERE (f.user1 = :user1 AND f.user2 = :user2) OR (f.user1 = :user2 AND f.user2 = :user1)", Friends.class)
                .setParameter("user1", currentUser)
                .setParameter("user2", friend)
                .getResultList();

        for (Friends friendship : friendships) {
            em.remove(friendship);
        }

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

        String jpql = "SELECT u FROM User u " +
                "WHERE u != :currentUser AND u.id NOT IN " +
                "(SELECT f.user1.id FROM Friends f WHERE f.user2 = :currentUser) AND u.id NOT IN " +
                "(SELECT f.user2.id FROM Friends f WHERE f.user1 = :currentUser)";

        return em.createQuery(jpql, User.class)
                .setParameter("currentUser", currentUser)
                .getResultList();
    }

    /**
     * Searches for users by email.
     */
    public void searchUsersByEmail() {
        if (searchEmail == null || searchEmail.trim().isEmpty()) {
            searchResults = Collections.emptyList();
            return;
        }

        User currentUser = userBean.getCurrentUser();
        if (currentUser == null) {
            searchResults = Collections.emptyList();
            return;
        }

        searchResults = em.createQuery("SELECT u FROM User u WHERE u.email = :email AND u != :currentUser", User.class)
                .setParameter("email", searchEmail.trim())
                .setParameter("currentUser", currentUser)
                .getResultList();
    }

    // Getters and Setters
    public UserBean getUserBean() {
        return userBean;
    }

    public User getFriendToAdd() {
        return friendToAdd;
    }

    public void setFriendToAdd(User friendToAdd) {
        this.friendToAdd = friendToAdd;
    }

    public String getSearchEmail() {
        return searchEmail;
    }

    public void setSearchEmail(String searchEmail) {
        this.searchEmail = searchEmail;
    }

    public List<User> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<User> searchResults) {
        this.searchResults = searchResults;
    }
}

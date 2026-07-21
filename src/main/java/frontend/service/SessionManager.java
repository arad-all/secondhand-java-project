package frontend.service;

/**
 * Holds the JWT token and current user's info for as long as the app is
 * running, i.e. until the user logs out. A simple singleton is enough for
 * this project — no need for anything fancier.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private String token;
    private Long userId;
    private String username;
    private String role;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void startSession(String token, Long userId, String username, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public void clear() {
        this.token = null;
        this.userId = null;
        this.username = null;
        this.role = null;
    }

    public boolean isLoggedIn() {
        return token != null;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}

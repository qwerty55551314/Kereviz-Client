package me.ksyz.accountmanager.auth;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class Account {
    public static final String TYPE_MICROSOFT = "microsoft";
    public static final String TYPE_CRACKED = "cracked";

    private String refreshToken;
    private String accessToken;
    private String username;
    private long unban;
    private String clientId;
    private String scope;
    private String type;

    public Account(String refreshToken, String accessToken, String username, String clientId, String scope) {
        this(refreshToken, accessToken, username, 0L, clientId, scope, TYPE_MICROSOFT);
    }

    public Account(String refreshToken, String accessToken, String username, long unban, String clientId, String scope) {
        this(refreshToken, accessToken, username, unban, clientId, scope, TYPE_MICROSOFT);
    }

    public Account(String refreshToken, String accessToken, String username, long unban, String clientId, String scope, String type) {
        this.accessToken = safe(accessToken);
        this.refreshToken = safe(refreshToken);
        this.username = safe(username);
        this.unban = unban;
        this.clientId = safe(clientId);
        this.scope = safe(scope);
        this.type = normalizeType(type);
    }

    public static Account cracked(String username) {
        return new Account("", "", username, 0L, "", "", TYPE_CRACKED);
    }

    public String getClientId() {
        return clientId;
    }

    public String getScope() {
        return scope;
    }

    public String getType() {
        return type;
    }

    public boolean isCracked() {
        return TYPE_CRACKED.equalsIgnoreCase(type);
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getUsername() {
        return username;
    }

    public long getUnban() {
        return unban;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = safe(refreshToken);
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = safe(accessToken);
    }

    public void setUsername(String username) {
        this.username = safe(username);
    }

    public void setUnban(long unban) {
        this.unban = unban;
    }

    public void setClientId(String clientId) {
        this.clientId = safe(clientId);
    }

    public void setScope(String scope) {
        this.scope = safe(scope);
    }

    public void setType(String type) {
        this.type = normalizeType(type);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeType(String type) {
        return TYPE_CRACKED.equalsIgnoreCase(type) ? TYPE_CRACKED : TYPE_MICROSOFT;
    }
}

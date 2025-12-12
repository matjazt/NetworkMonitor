package com.matjazt.networkmonitor.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matjazt.networkmonitor.entity.Account;
import com.matjazt.networkmonitor.entity.AccountNetwork;
import com.matjazt.networkmonitor.entity.Network;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Data Access Object for user authentication and network authorization.
 * Handles user credential verification and network access permissions.
 */
@Stateless
public class AccountManagementDAO {

    private static final Logger logger = LoggerFactory.getLogger(AccountManagementDAO.class);

    @PersistenceContext(unitName = "NetworkMonitorPU")
    private EntityManager em;

    /**
     * Authenticates a user and returns the list of networks they can manage.
     * 
     * @param username The username to authenticate
     * @param password The plain-text password (will be compared with stored hash)
     * @return List of networks the account has access to
     * @throws AuthenticationException if username/password combination is invalid
     */
    public List<Network> getNetworksForAccount_DONT_USE_THIS_ONE(String username, String password)
            throws AuthenticationException {

        logger.debug("Attempting authentication for username: {}", username);

        // Step 1: Find account by username
        Account account = findAccountByUsername(username);
        if (account == null || !verifyPassword(password, account.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password");
        }

        // Step 3: Update last seen timestamp
        updateLastSeen(account);

        // Step 4: Retrieve networks for this account
        List<Network> networks = getNetworksForAccount(account);

        logger.info("User {} authenticated successfully, has access to {} network(s)",
                username, networks.size());

        return networks;
    }

    /**
     * Find an account by username.
     */
    public Account findAccountByUsername(String username) {
        try {
            TypedQuery<Account> query = em.createQuery(
                    "SELECT a FROM Account a WHERE a.username = :username", Account.class);
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Verify password against stored hash.
     * 
     * TODO: Implement proper password hashing (BCrypt, Argon2, etc.)
     * For now, this is a placeholder that compares plain text.
     */
    public boolean verifyPassword(String plainPassword, String storedHash) {
        // TEMPORARY: Plain text comparison
        // TODO: Replace with proper password verification:
        // return BCrypt.checkpw(plainPassword, storedHash);
        return plainPassword.equals(storedHash);
    }

    /**
     * Update the account's last_seen timestamp.
     */
    public void updateLastSeen(Account account) {
        account.setLastSeen(LocalDateTime.now());
        em.merge(account);
    }

    /**
     * Retrieve all networks that the account has access to.
     */
    public List<Network> getNetworksForAccount(Account account) {
        TypedQuery<AccountNetwork> query = em.createQuery(
                "SELECT an.network FROM AccountNetwork an " +
                        " WHERE an.account.id = :accountId " +
                        " ORDER BY an.network.name ",
                AccountNetwork.class);
        query.setParameter("accountId", account.getId());
        return query.getResultList().stream()
                .map(AccountNetwork::getNetwork)
                .toList();
    }

    /**
     * Exception thrown when authentication fails.
     */
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}

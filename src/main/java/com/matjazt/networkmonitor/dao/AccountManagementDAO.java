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
     * Verify password against stored BCrypt hash.
     */
    public boolean verifyPassword(String plainPassword, String storedHash) {
        try {
            return org.mindrot.jbcrypt.BCrypt.checkpw(plainPassword, storedHash);
        } catch (Exception e) {
            var hashedPassword = hashPassword(plainPassword);
            logger.warn("Password verification failed", e);
            logger.debug("computed hash you can put into database: {}", hashedPassword);
            return false;
        }
    }

    /**
     * Hash a plain text password using BCrypt.
     * Use this when creating or updating accounts.
     * 
     * @param plainPassword The plain text password
     * @return BCrypt hash string (includes salt)
     */
    public String hashPassword(String plainPassword) {
        // 8 rounds = should be pretty fast, adjust as needed for security/performance
        // balance
        return org.mindrot.jbcrypt.BCrypt.hashpw(plainPassword, org.mindrot.jbcrypt.BCrypt.gensalt(8));
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

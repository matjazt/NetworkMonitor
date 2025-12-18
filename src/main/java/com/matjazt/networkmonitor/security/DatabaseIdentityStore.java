package com.matjazt.networkmonitor.security;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matjazt.networkmonitor.dao.AccountManagementDAO;
import com.matjazt.networkmonitor.entity.AccountEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

/**
 * Custom IdentityStore that validates credentials against the database
 * and creates an enriched AccountPrincipal.
 */
@ApplicationScoped
public class DatabaseIdentityStore implements IdentityStore {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseIdentityStore.class);

    @Inject
    private AccountManagementDAO accountManagementDAO;

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof UsernamePasswordCredential) {
            UsernamePasswordCredential upCredential = (UsernamePasswordCredential) credential;
            String username = upCredential.getCaller();
            String password = upCredential.getPasswordAsString();

            try {
                // Find account by username
                AccountEntity account = accountManagementDAO.findAccountByUsername(username);

                if (account != null && accountManagementDAO.verifyPassword(password, account.getPasswordHash())) {
                    // Update last seen
                    accountManagementDAO.updateLastSeen(account);

                    // Get networks
                    List<NetworkEntity> networks = accountManagementDAO.getNetworksForAccount(account);

                    // Create enriched principal
                    AccountPrincipal principal = new AccountPrincipal(account, networks);

                    // Get role from account type
                    String role = account.getAccountType().getName();

                    logger.info("User {} authenticated successfully. Access to {} network(s), role: {}",
                            username, networks.size(), role);

                    return new CredentialValidationResult(principal, Set.of("Authenticated", role));
                }

                logger.warn("Authentication failed for user: {}", username);
            } catch (Exception e) {
                logger.error("Authentication error for user: {}", username, e);
            }
        }

        return CredentialValidationResult.INVALID_RESULT;
    }
}
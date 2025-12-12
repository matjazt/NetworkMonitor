package com.matjazt.networkmonitor.security;

import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matjazt.networkmonitor.dao.AccountManagementDAO;
import com.matjazt.networkmonitor.entity.Account;
import com.matjazt.networkmonitor.entity.Network;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP Basic Authentication mechanism using Jakarta Security API.
 * Authenticates users via username/password and creates an enriched
 * AccountPrincipal.
 */
@ApplicationScoped
public class BasicAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationMechanism.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BASIC_PREFIX = "Basic ";

    @Inject
    private AccountManagementDAO accountManagementDAO;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
            HttpServletResponse response,
            HttpMessageContext context)
            throws AuthenticationException {

        // Check for Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BASIC_PREFIX)) {
            // Extract and decode Basic auth credentials
            String base64Credentials = authHeader.substring(BASIC_PREFIX.length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials));

            // Split username:password
            String[] parts = credentials.split(":", 2);
            if (parts.length == 2) {
                String username = parts[0];
                String password = parts[1];

                try {
                    // Authenticate using AccountManagementDAO
                    Account account = accountManagementDAO.findAccountByUsername(username);

                    if (account != null && accountManagementDAO.verifyPassword(password, account.getPasswordHash())) {
                        // Update last seen timestamp
                        accountManagementDAO.updateLastSeen(account);

                        // Get networks for this account
                        List<Network> networks = accountManagementDAO.getNetworksForAccount(account);

                        // Create enriched principal
                        AccountPrincipal principal = new AccountPrincipal(account, networks);

                        logger.info("User {} authenticated successfully. Access to {} network(s)",
                                username, networks.size());

                        // Notify container of successful authentication
                        return context.notifyContainerAboutLogin(principal,
                                Set.of(account.getAccountType().getName()));
                    }

                    logger.warn("Authentication failed for user: {}", username);
                } catch (Exception e) {
                    logger.error("Authentication error for user: {}", username, e);
                }
            }
        }

        // No valid credentials - challenge the client
        if (context.isProtected()) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Network Monitor\"");
            return context.responseUnauthorized();
        }

        return context.doNothing();
    }
}

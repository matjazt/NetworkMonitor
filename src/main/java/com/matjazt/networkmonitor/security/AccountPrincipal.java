package com.matjazt.networkmonitor.security;

import com.matjazt.networkmonitor.entity.Account;
import com.matjazt.networkmonitor.entity.Network;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Custom Principal that holds the authenticated Account and their accessible
 * Networks.
 * This enriched principal provides direct access to user context without
 * additional queries.
 */
public class AccountPrincipal implements Principal {

    private final Account account;
    private final List<Network> networks;

    public AccountPrincipal(Account account, List<Network> networks) {
        this.account = account;
        this.networks = networks != null ? networks : Collections.emptyList();
    }

    @Override
    public String getName() {
        return account.getUsername();
    }

    public Account getAccount() {
        return account;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public Long getAccountId() {
        return account.getId();
    }

    public String getFullName() {
        return account.getFullName();
    }

    public String getUserType() {
        return account.getAccountType().getName();
    }

    @Override
    public String toString() {
        return "AccountPrincipal{" +
                "username='" + account.getUsername() + '\'' +
                ", fullName='" + account.getFullName() + '\'' +
                ", accountType='" + account.getAccountType().getName() + '\'' +
                ", networks=" + networks.size() +
                '}';
    }
}

package com.matjazt.networkmonitor.security;

import java.util.Collections;
import java.util.List;

import com.matjazt.networkmonitor.entity.AccountEntity;
import com.matjazt.networkmonitor.entity.NetworkEntity;

import jakarta.security.enterprise.CallerPrincipal;

/**
 * Custom Principal that holds the authenticated Account and their accessible
 * Networks.
 * This enriched principal provides direct access to user context without
 * additional queries.
 */
public class AccountPrincipal extends CallerPrincipal {

    private final AccountEntity account;
    private final List<NetworkEntity> networks;

    public AccountPrincipal(AccountEntity account, List<NetworkEntity> networks) {
        super(account.getUsername());
        this.account = account;
        this.networks = networks != null ? networks : Collections.emptyList();
    }

    @Override
    public String getName() {
        return account.getUsername();
    }

    public AccountEntity getAccount() {
        return account;
    }

    public List<NetworkEntity> getNetworks() {
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

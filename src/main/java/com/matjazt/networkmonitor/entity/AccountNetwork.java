package com.matjazt.networkmonitor.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Join table entity representing which networks an account can manage.
 * Many-to-many relationship between accounts and networks.
 */
@Entity
@Table(name = "account_network")
public class AccountNetwork implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private Network network;

    // Constructors
    public AccountNetwork() {
    }

    public AccountNetwork(Account account, Network network) {
        this.account = account;
        this.network = network;
    }

    // Getters and Setters
    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AccountNetwork that = (AccountNetwork) o;
        return Objects.equals(account, that.account) &&
                Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, network);
    }

    @Override
    public String toString() {
        return "AccountNetwork{" +
                "accountId=" + (account != null ? account.getId() : null) +
                ", networkId=" + (network != null ? network.getId() : null) +
                '}';
    }
}

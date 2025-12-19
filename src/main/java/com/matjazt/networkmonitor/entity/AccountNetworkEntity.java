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
public class AccountNetworkEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_id", nullable = false)
    private NetworkEntity network;

    // Constructors
    public AccountNetworkEntity() {
    }

    public AccountNetworkEntity(AccountEntity account, NetworkEntity network) {
        this.account = account;
        this.network = network;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }

    public NetworkEntity getNetwork() {
        return network;
    }

    public void setNetwork(NetworkEntity network) {
        this.network = network;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AccountNetworkEntity that = (AccountNetworkEntity) o;
        return Objects.equals(id, that.id);
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

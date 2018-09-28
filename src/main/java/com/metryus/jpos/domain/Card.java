package com.metryus.jpos.domain;

import java.math.BigDecimal;

/**
 * @author Maxim Maximchuk
 * created on 24.09.18
 */
public class Card {

    private String number;
    private int balance;
    private Status status;

    public enum Status {
        ACTIVE, BLOCKED
    }

    public Card(String number, int balance, Status status) {
        this.number = number;
        this.balance = balance;
        this.status = status;
    }

    public String getNumber() {
        return number;
    }

    public int getBalance() {
        return balance;
    }

    public Status getStatus() {
        return status;
    }

    public void debit(int amount) {
        balance = balance - amount;
    }
}

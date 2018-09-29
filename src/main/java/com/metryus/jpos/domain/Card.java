package com.metryus.jpos.domain;

import java.time.LocalDate;

/**
 * @author Maxim Maximchuk
 * created on 24.09.18
 */
public class Card {

    private String number;
    private int balance;
    private Status status;
    private LocalDate expire;

    public enum Status {
        ACTIVE, BLOCKED
    }

    public Card(String number, int balance, Status status, LocalDate expire) {
        this.number = number;
        this.balance = balance;
        this.status = status;
        this.expire = expire;
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

    public LocalDate getExpire() {
        return expire;
    }

    public void debit(int amount) {
        balance = balance - amount;
    }
}

package com.metryus.jpos.domain;

/**
 * @author Maxim Maximchuk
 * created on 24.09.18
 */
public enum ResponseStatus {

    SUCCESSFUL("00"),
    INSUFFICIENT_FUNDS("51"),
    INVALID_TRANSACTION("12"),
    EXPIRED_CARD("54"),
    INVALID_ACCOUNT_NUMBER("14"),
    RESTRICTED_CARD("62");

    private String code;

    ResponseStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

package com.metryus.jpos.participant;

import com.metryus.jpos.domain.Card;
import com.metryus.jpos.domain.ResponseStatus;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.jpos.transaction.ContextConstants.REQUEST;
import static org.jpos.transaction.ContextConstants.RESPONSE;

/**
 * @author Maxim Maximchuk
 * created on 24.09.18
 */
public class MockCardProcessing implements TransactionParticipant {

    private static Set<String> supportedMTI = new HashSet<>();

    private Map<String, Card> cards = new ConcurrentHashMap<>();

    static {
        supportedMTI.add("0100");
        supportedMTI.add("0200");
        supportedMTI.add("0800");
    }

    public MockCardProcessing() {
        cards.put("5218572220365428", new Card("5218572220365428", BigDecimal.valueOf(100), Card.Status.ACTIVE));
    }

    @Override
    public int prepare(long id, Serializable context) {
        try {
            return supportedMTI.contains(getRequest(context).getMTI()) ? PREPARED : ABORTED;
        } catch (ISOException e) {
            log(context, e);
            return ABORTED;
        }
    }

    @Override
    public void commit(long id, Serializable context) {
        try {
            ISOMsg request = getRequest(context);
            ISOMsg response;
            switch (request.getMTI()) {
                case "0100":
                    response = handleAuthorizationRequest(request);
                    break;
                case "0200":
                    if (request.getString(3).equals("500000")) {
                        response = handlePaymentFromAccount(request);
                    } else {
                        response = setResponseCode(request, ResponseStatus.INVALID_TRANSACTION);
                    }
                    break;
                case "0800":
                    response = handleEchoMessage(request);
                    break;
                default:
                    response = setResponseCode(request, ResponseStatus.INVALID_TRANSACTION);
            }

            setResponse(context, response);
        } catch (ISOException e) {
            log(context, e);
        }
    }

    @Override
    public void abort(long id, Serializable context) {

    }

    private ISOMsg handleEchoMessage(ISOMsg msg) throws ISOException {
        msg.setResponseMTI();
        return msg;
    }

    private ISOMsg handleAuthorizationRequest(ISOMsg msg) throws ISOException {
        Card card = cards.get(getAccountNumber(msg));

        BigDecimal amount = getTransactionAmount(msg);

        if (card.getStatus() == Card.Status.BLOCKED) {
            setResponseCode(msg, ResponseStatus.RESTRICTED_CARD);
        }

        if (card.getStatus() == Card.Status.BLOCKED) {
            setResponseCode(msg, ResponseStatus.RESTRICTED_CARD);
        }

        if (card.getBalance().compareTo(amount) >= 0) {
            setResponseCode(msg, ResponseStatus.SUCCESSFUL);
        } else {
            setResponseCode(msg, ResponseStatus.INSUFFICIENT_FUNDS);
        }

        return msg;
    }

    private ISOMsg handlePaymentFromAccount(ISOMsg msg) throws ISOException {
        Card card = cards.get(getAccountNumber(msg));

        BigDecimal amount = getTransactionAmount(msg);

        if (card.getStatus() == Card.Status.BLOCKED) {
            setResponseCode(msg, ResponseStatus.RESTRICTED_CARD);
        }

        if (card.getBalance().compareTo(amount) < 0) {
            setResponseCode(msg, ResponseStatus.INSUFFICIENT_FUNDS);
        } else {
            card.debit(amount);
            setResponseCode(msg, ResponseStatus.SUCCESSFUL);
        }

        return msg;
    }

    private String getAccountNumber(ISOMsg msg) {
        return msg.getString(2);
    }

    private BigDecimal getTransactionAmount(ISOMsg msg) {
        return new BigDecimal(msg.getString(4));
    }

    private ISOMsg setResponseCode(ISOMsg msg, ResponseStatus responseStatus) {
        msg.set(39, responseStatus.getCode());
        return msg;
    }


    private ISOMsg getRequest(Serializable context) {
        return (ISOMsg) ((Context) context).get(REQUEST.toString());
    }

    private void setResponse(Serializable context, ISOMsg msg) throws ISOException {
        msg.setResponseMTI();
        ((Context) context).put(RESPONSE.toString(), msg);
    }

    private void log(Serializable context, Object o) {
        ((Context) context).log(o);
    }

}

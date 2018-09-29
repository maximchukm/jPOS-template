package com.metryus.jpos.participant;

import com.metryus.jpos.domain.Card;
import com.metryus.jpos.domain.ResponseStatus;
import org.jpos.core.CardHolder;
import org.jpos.core.InvalidCardException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        } catch (Exception e) {
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

    private ISOMsg handleAuthorizationRequest(ISOMsg msg) throws InvalidCardException, ISOException {
        Card card = getCard(msg);

        int amount = getTransactionAmount(msg);

        if (card.getStatus() == Card.Status.BLOCKED) {
            setResponseCode(msg, ResponseStatus.RESTRICTED_CARD);
        }

        if (card.getStatus() == Card.Status.BLOCKED) {
            setResponseCode(msg, ResponseStatus.RESTRICTED_CARD);
        }

        if (card.getBalance() - amount >= 0) {
            setResponseCode(msg, ResponseStatus.SUCCESSFUL);
        } else {
            setResponseCode(msg, ResponseStatus.INSUFFICIENT_FUNDS);
        }

        return msg;
    }

    private ISOMsg handlePaymentFromAccount(ISOMsg msg) throws InvalidCardException, ISOException {
        Card card = getCard(msg);

        int amount = getTransactionAmount(msg);

        if (card.getStatus() == Card.Status.BLOCKED) {
            setResponseCode(msg, ResponseStatus.RESTRICTED_CARD);
        }

        if (card.getBalance() - amount <= 0) {
            setResponseCode(msg, ResponseStatus.INSUFFICIENT_FUNDS);
        } else {
            card.debit(amount);
            setResponseCode(msg, ResponseStatus.SUCCESSFUL);
        }

        return msg;
    }

    private Card getCard(ISOMsg msg) throws InvalidCardException, ISOException {
        Card card;
        if (msg.hasField(35)) {
            Matcher matcher = Pattern.compile("^\\;(.{0,19})=(\\d{4})(.*)\\?").matcher(msg.getString(35));
            if (matcher.find()) {
                String pan = matcher.group(1);
                String exp = matcher.group(2);
                String trailer = matcher.group(3);

                if (cards.containsKey(pan)) {
                    card = cards.get(pan);
                } else {
                    card = addCard(pan);
                }
            } else {
                throw new ISOException("invalid track2 data");
            }
        } else {
            card = cards.get(new CardHolder(msg).getPAN());
        }
        return card;
    }

    private int getTransactionAmount(ISOMsg msg) {
        return Integer.parseInt(msg.getString(4));
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

    private Card addCard(String cardNumber) {
        Card card = new Card(cardNumber, 10000, Card.Status.ACTIVE);
        cards.put(cardNumber, card);
        return card;
    }

    private void log(Serializable context, Object o) {
        ((Context) context).log(o);
    }

}

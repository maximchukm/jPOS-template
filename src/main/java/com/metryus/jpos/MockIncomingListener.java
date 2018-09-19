package com.metryus.jpos;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.iso.IncomingListener;

/**
 * @author Maxim Maximchuk
 * created on 19.09.18
 */
public class MockIncomingListener extends IncomingListener {
    @Override
    public boolean process(ISOSource src, ISOMsg m) {
        return super.process(src, m);
    }
}

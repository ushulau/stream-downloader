package com.gplex;

import org.junit.Test;

import java.security.Key;
import java.security.Security;

/**
 * Created by Vlad S. on 10/30/17.
 */
public class CryptoUtilsTest {
    @Test(expected = Exception.class)
    public void testPublicKeyLoad() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Key key = CryptoUtils.getPublicKey("key.key");

    }
}
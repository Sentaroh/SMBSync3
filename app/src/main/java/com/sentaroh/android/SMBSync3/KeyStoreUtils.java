/*
The MIT License (MIT)
Copyright (c) 2020 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/
package com.sentaroh.android.SMBSync3;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.x500.X500Principal;

class KeyStoreUtils {
    private static Logger log= LoggerFactory.getLogger(KeyStoreUtils.class);
    static final private String PROVIDER = "AndroidKeyStore";

    public static boolean isStoredKeyExists(Context context, String alias) {
        boolean result=false;
        try {
            KeyStore keyStore = KeyStore.getInstance(PROVIDER);
            keyStore.load(null);
            result=keyStore.containsAlias(alias);
        } catch(Exception e) {}
        if (log.isDebugEnabled()) log.debug("isStoredKeyExists result="+result);
        return result;
    }

    public static SecretKey getStoredKey(Context context, String alias) throws Exception {
        if (log.isDebugEnabled()) log.debug("getStoredKey entered");
        KeyStore keyStore = KeyStore.getInstance(PROVIDER);
        keyStore.load(null);
        SecretKey privateKey =null;
        if (!keyStore.containsAlias(alias)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER);
            keyGenerator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT| KeyProperties.PURPOSE_DECRYPT)
                    .setCertificateSubject(new X500Principal("CN="+alias))
                    .setCertificateSerialNumber(BigInteger.ONE)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(false)
                    .build());
            privateKey =keyGenerator.generateKey();
        } else {
            privateKey = (SecretKey) keyStore.getKey(alias, null);
        }
        if (log.isDebugEnabled()) log.debug("getStoredKey exit");

        return privateKey;
    }

}

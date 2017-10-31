package com.gplex;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;


public final class CryptoUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoUtils.class);




    public static Key getKey(File file){
        InputStream passKeyStream = null;
        try {
            passKeyStream = new FileInputStream(file);
            byte[] keyBytes = StreamUtils.copyToByteArray(passKeyStream);

            KeyFactory keyFactory = KeyFactory.getInstance("AES");
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);

            if (publicKey instanceof RSAPublicKey) {
                return (RSAPublicKey) publicKey;
            } else {
                throw new InvalidKeySpecException("Unexpected key type");
            }
        }catch (Exception e){
            LOGGER.error("", e);

        } finally {
            if(passKeyStream != null){
                try {
                    passKeyStream.close();
                }catch (Exception e){
                    LOGGER.error("",e);
                }
            }

        }
        return null;
    }


    public static Key getKey(String base64StringValue){
        return new SecretKeySpec(Base64.decodeBase64(base64StringValue), "AES");
    }

    /**
     * Prevent this class from being instantiated, as it should always be
     * accessed statically
     */
    private CryptoUtils() {

    }

    /**
     * Encrypt using specified key
     *
     * @param value
     *            Value to encrypt
     * @param key
     *            Key used to encrypt
     * @return Encrypted representation of value
     */
    public static String encryptValue(String value, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(value.getBytes(Charset.defaultCharset()));
            return Base64.encodeBase64String(encrypted);

        } catch (Exception e) {
            LOGGER.warn("Encription of [" + value + "] failed.", e);
            return null;
        }
    }

    /**
     * Decrypt using specified key
     *
     * @param value
     *            Value to decrypt
     * @param key
     *            Key used to decrypt
     * @return Decrypted representation of value
     */
    public static String decryptValue(String value, Key key) {
        try {
            byte[] valueArr = Base64.decodeBase64(value);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(valueArr);
            return new String(decrypted, Charset.defaultCharset());

        } catch (Exception e) {
            LOGGER.warn("Description of [" + value + "] failed.", e);
            return null;
        }
    }


    /**
     *
     * @param file
     * @param key
     * @return
     */
    public static byte[] decryptValue(File file, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES", "BC");
            cipher.init(Cipher.DECRYPT_MODE, key);
            FileInputStream fis = new FileInputStream(file);
            CipherInputStream in = new CipherInputStream(fis, cipher);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] b = new byte[1024];
            int numberOfBytedRead;
            while ((numberOfBytedRead = in.read(b)) >= 0) {
                baos.write(b, 0, numberOfBytedRead);
            }

            return b;

        } catch (Exception e) {
            LOGGER.error("", e);
            return null;
        }
    }


    public static Key getPublicKey(String filename)
            throws Exception {
        throw new RuntimeException("Not implemented");
    }

}

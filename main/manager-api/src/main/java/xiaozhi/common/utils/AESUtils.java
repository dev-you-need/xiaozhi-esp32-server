package xiaozhi.common.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * Шифрование AES
     * 
     * @param key       ключ (16, 24 или 32 байта)
     * @param plainText строка для шифрования
     * @return зашифрованная строка в Base64
     */
    public static String encrypt(String key, String plainText) {
        try {
            // Убедиться, что длина ключа составляет 16, 24 или 32 байта
            byte[] keyBytes = padKey(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * Дешифрование AES
     * 
     * @param key           ключ (16, 24 или 32 байта)
     * @param encryptedText зашифрованная строка в Base64
     * @return расшифрованная строка
     */
    public static String decrypt(String key, String encryptedText) {
        try {
            // Убедиться, что длина ключа составляет 16, 24 или 32 байта
            byte[] keyBytes = padKey(key.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * Дополнение ключа до нужной длины (16, 24 или 32 байта)
     * 
     * @param keyBytes исходный массив байтов ключа
     * @return дополненный массив байтов ключа
     */
    private static byte[] padKey(byte[] keyBytes) {
        int keyLength = keyBytes.length;
        if (keyLength == 16 || keyLength == 24 || keyLength == 32) {
            return keyBytes;
        }

        // Если длина ключа недостаточна — дополнить нулями; если превышает — обрезать до 32 байт
        byte[] paddedKey = new byte[32];
        System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyLength, 32));
        return paddedKey;
    }
}

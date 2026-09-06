package xiaozhi.common.utils;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Утилита шифрования SM2 (шестнадцатеричный формат, совместим с проектом chancheng-archive-service)
 */
public class SM2Utils {

    /**
     * Константа публичного ключа
     */
    public static final String KEY_PUBLIC_KEY = "publicKey";
    /**
     * Константа приватного ключа
     */
    public static final String KEY_PRIVATE_KEY = "privateKey";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Алгоритм шифрования SM2
     *
     * @param publicKey шестнадцатеричный публичный ключ
     * @param data      открытые данные
     * @return шестнадцатеричный шифротекст
     */
    public static String encrypt(String publicKey, String data) {
        try {
            // Получение параметров кривой SM2
            X9ECParameters sm2ECParameters = GMNamedCurves.getByName("sm2p256v1");
            // Построение параметров алгоритма ECC
            ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
            //Извлечение точки публичного ключа
            ECPoint pukPoint = sm2ECParameters.getCurve().decodePoint(Hex.decode(publicKey));
            // Префикс 02 или 03 означает сжатый ключ, 04 — несжатый
            ECPublicKeyParameters publicKeyParameters = new ECPublicKeyParameters(pukPoint, domainParameters);

            SM2Engine sm2Engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            // Установка SM2 в режим шифрования
            sm2Engine.init(true, new ParametersWithRandom(publicKeyParameters, new SecureRandom()));

            byte[] in = data.getBytes(StandardCharsets.UTF_8);
            byte[] arrayOfBytes = sm2Engine.processBlock(in, 0, in.length);
            return Hex.toHexString(arrayOfBytes);
        } catch (Exception e) {
            throw new RuntimeException("SM2加密失败", e);
        }
    }

    /**
     * Алгоритм дешифрования SM2
     *
     * @param privateKey шестнадцатеричный приватный ключ
     * @param cipherData шестнадцатеричные зашифрованные данные
     * @return открытый текст
     */
    public static String decrypt(String privateKey, String cipherData) {
        try {
            // При шифровании через BC-библиотеку шифротекст начинается с 04
            if (!cipherData.startsWith("04")) {
                cipherData = "04" + cipherData;
            }
            byte[] cipherDataByte = Hex.decode(cipherData);
            BigInteger privateKeyD = new BigInteger(privateKey, 16);
            //Получение параметров кривой SM2
            X9ECParameters sm2ECParameters = GMNamedCurves.getByName("sm2p256v1");
            //Построение параметров домена
            ECDomainParameters domainParameters = new ECDomainParameters(sm2ECParameters.getCurve(), sm2ECParameters.getG(), sm2ECParameters.getN());
            ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(privateKeyD, domainParameters);

            SM2Engine sm2Engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
            // Установка SM2 в режим дешифрования
            sm2Engine.init(false, privateKeyParameters);

            byte[] arrayOfBytes = sm2Engine.processBlock(cipherDataByte, 0, cipherDataByte.length);
            return new String(arrayOfBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM2解密失败", e);
        }
    }

    /**
     * Генерация пары ключей
     */
    public static Map<String, String> createKey() {
        try {
            ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
            // Получение генератора пар ключей эллиптической кривой
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            // Инициализация генератора параметрами SM2
            kpg.initialize(sm2Spec);
            // Получение пары ключей
            KeyPair keyPair = kpg.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            BCECPublicKey p = (BCECPublicKey) publicKey;
            PrivateKey privateKey = keyPair.getPrivate();
            BCECPrivateKey s = (BCECPrivateKey) privateKey;
            
            Map<String, String> result = new HashMap<>();
            result.put(KEY_PUBLIC_KEY, Hex.toHexString(p.getQ().getEncoded(false)));
            result.put(KEY_PRIVATE_KEY, Hex.toHexString(s.getD().toByteArray()));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("生成SM2密钥对失败", e);
        }
    }


}
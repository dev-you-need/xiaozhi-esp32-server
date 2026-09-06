package xiaozhi.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AESUtilsTest {

    @Test
    public void testEncryptAndDecrypt() {
        String key = "xiaozhi1234567890";
        String plainText = "Hello, 小智!";

        System.out.println("原始文本: " + plainText);
        System.out.println("密钥: " + key);

        // Шифрование
        String encrypted = AESUtils.encrypt(key, plainText);
        System.out.println("加密结果: " + encrypted);

        // Расшифровка
        String decrypted = AESUtils.decrypt(key, encrypted);
        System.out.println("解密结果: " + decrypted);

        // Проверка
        assertEquals(plainText, decrypted, "加解密结果应该一致");
        System.out.println("加解密一致性: " + plainText.equals(decrypted));
    }

    @Test
    public void testDifferentKeyLengths() {
        String[] keys = {
                "1234567890123456", // 16 бит
                "123456789012345678901234", // 24 бита
                "12345678901234567890123456789012", // 32 бита
                "short", // короткий ключ
                "verylongkeythatwillbetruncatedto32bytes" // длинный ключ
        };

        String plainText = "测试文本";

        for (String key : keys) {
            String encrypted = AESUtils.encrypt(key, plainText);
            String decrypted = AESUtils.decrypt(key, encrypted);
            assertEquals(plainText, decrypted, "密钥长度: " + key.length());
        }
    }

    @Test
    public void testSpecialCharacters() {
        String key = "xiaozhi1234567890";
        String[] testTexts = {
                "Hello World",
                "你好世界",
                "Hello, 小智!",
                "特殊字符: !@#$%^&*()",
                "数字123和中文混合",
                "Emoji: 😀🎉🚀",
                "空字符串测试",
                ""
        };

        for (String text : testTexts) {
            String encrypted = AESUtils.encrypt(key, text);
            String decrypted = AESUtils.decrypt(key, encrypted);
            assertEquals(text, decrypted, "测试文本: " + text);
        }
    }

    @Test
    public void testCrossLanguageCompatibility() {
        // Это зашифрованные результаты, сгенерированные версией Python, для тестирования совместимости между языками
        String key = "xiaozhi1234567890";
        String plainText = "Hello, 小智!";

        // Зашифрованный результат, сгенерированный версией Python (необходимо получить после запуска теста Python)
        // String pythonEncrypted = "Зашифрованный результат, полученный из теста Python";
        // String decrypted = AESUtils.decrypt(key, pythonEncrypted);
        // assertEquals(plainText, decrypted, "Java должна расшифровать результат шифрования Python");

        // Генерация результата шифрования Java для тестирования Python
        String javaEncrypted = AESUtils.encrypt(key, plainText);
        System.out.println("Java加密结果供Python测试: " + javaEncrypted);
    }
}
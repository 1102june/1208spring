package com.example.youth.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API 키 암호화/복호화 유틸리티
 * AES-256 암호화를 사용하여 민감한 정보를 암호화합니다.
 */
public class CryptoUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final int KEY_SIZE = 256;
    
    /**
     * 암호화 키 생성 (로컬 개발용)
     * 실제 운영 환경에서는 환경 변수에서 키를 가져와야 합니다.
     * 
     * @return Base64로 인코딩된 암호화 키
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE, new SecureRandom());
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
    
    /**
     * Base64로 인코딩된 키 문자열을 SecretKey 객체로 변환
     */
    private static SecretKey getSecretKey(String base64Key) {
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }
    
    /**
     * 평문을 암호화하여 Base64 문자열로 반환
     * 
     * @param plainText 암호화할 평문
     * @param base64Key Base64로 인코딩된 암호화 키
     * @return 암호화된 Base64 문자열
     */
    public static String encrypt(String plainText, String base64Key) {
        try {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            
            SecretKey secretKey = getSecretKey(base64Key);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 암호화된 Base64 문자열을 복호화하여 평문으로 반환
     * 
     * @param encryptedText 암호화된 Base64 문자열
     * @param base64Key Base64로 인코딩된 암호화 키
     * @return 복호화된 평문
     */
    public static String decrypt(String encryptedText, String base64Key) {
        try {
            if (encryptedText == null || encryptedText.isEmpty()) {
                return encryptedText;
            }
            
            // 암호화되지 않은 경우 그대로 반환 (하위 호환성)
            if (!isEncrypted(encryptedText)) {
                return encryptedText;
            }
            
            SecretKey secretKey = getSecretKey(base64Key);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 문자열이 암호화된 형식인지 확인
     * Base64 형식이고 길이가 충분한지 확인
     */
    private static boolean isEncrypted(String text) {
        if (text == null || text.length() < 20) {
            return false;
        }
        
        try {
            // Base64 형식인지 확인
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 암호화 키를 환경 변수나 시스템 속성에서 가져오기
     * 우선순위: 환경 변수 > 시스템 속성 > 기본값 (로컬 개발용)
     */
    public static String getEncryptionKey() {
        // 환경 변수에서 먼저 확인
        String key = System.getenv("ENCRYPTION_KEY");
        if (key != null && !key.isEmpty()) {
            return key;
        }
        
        // 시스템 속성에서 확인
        key = System.getProperty("encryption.key");
        if (key != null && !key.isEmpty()) {
            return key;
        }
        
        // 기본값 반환 (로컬 개발용 - 실제 운영에서는 환경 변수 필수)
        throw new RuntimeException(
            "ENCRYPTION_KEY 환경 변수가 설정되지 않았습니다. " +
            ".env 파일에 ENCRYPTION_KEY를 추가하세요."
        );
    }
}


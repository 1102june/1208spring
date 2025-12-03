package com.example.youth.util;

/**
 * API 키 암호화/복호화 명령줄 도구
 * 
 * 사용법:
 *   - 키 생성: java ApiKeyCryptoTool generate
 *   - 암호화: java ApiKeyCryptoTool encrypt <평문_API_키> <암호화_키>
 *   - 복호화: java ApiKeyCryptoTool decrypt <암호화된_API_키> <암호화_키>
 */
public class ApiKeyCryptoTool {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }
        
        String command = args[0];
        
        try {
            switch (command.toLowerCase()) {
                case "generate":
                    if (args.length != 1) {
                        System.err.println("사용법: java ApiKeyCryptoTool generate");
                        System.exit(1);
                    }
                    generateKey();
                    break;
                    
                case "encrypt":
                    if (args.length != 3) {
                        System.err.println("사용법: java ApiKeyCryptoTool encrypt <평문_API_키> <암호화_키>");
                        System.exit(1);
                    }
                    encrypt(args[1], args[2]);
                    break;
                    
                case "decrypt":
                    if (args.length != 3) {
                        System.err.println("사용법: java ApiKeyCryptoTool decrypt <암호화된_API_키> <암호화_키>");
                        System.exit(1);
                    }
                    decrypt(args[1], args[2]);
                    break;
                    
                default:
                    System.err.println("알 수 없는 명령어: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void printUsage() {
        System.out.println("API 키 암호화/복호화 도구");
        System.out.println("");
        System.out.println("사용법:");
        System.out.println("  java ApiKeyCryptoTool generate                    # 암호화 키 생성");
        System.out.println("  java ApiKeyCryptoTool encrypt <평문> <키>        # API 키 암호화");
        System.out.println("  java ApiKeyCryptoTool decrypt <암호화> <키>      # API 키 복호화");
        System.out.println("");
        System.out.println("예제:");
        System.out.println("  java ApiKeyCryptoTool generate");
        System.out.println("  java ApiKeyCryptoTool encrypt \"AIzaSy...\" \"암호화_키\"");
        System.out.println("  java ApiKeyCryptoTool decrypt \"암호화된_키\" \"암호화_키\"");
    }
    
    private static void generateKey() throws Exception {
        String key = CryptoUtil.generateKey();
        System.out.println(key);
    }
    
    private static void encrypt(String plainText, String encryptionKey) {
        String encrypted = CryptoUtil.encrypt(plainText, encryptionKey);
        System.out.println(encrypted);
    }
    
    private static void decrypt(String encryptedText, String encryptionKey) {
        String decrypted = CryptoUtil.decrypt(encryptedText, encryptionKey);
        System.out.println(decrypted);
    }
}


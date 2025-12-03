# Gemini API 키 암호화 스크립트
# 로컬에서 API 키를 암호화하여 서버에 배포할 수 있도록 준비합니다.

param(
    [Parameter(Mandatory=$true)]
    [string]$PlainApiKey,
    
    [Parameter(Mandatory=$false)]
    [string]$EncryptionKey = ""
)

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Gemini API 키 암호화 도구" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 암호화 키 확인 또는 생성
if ([string]::IsNullOrEmpty($EncryptionKey)) {
    Write-Host "[1/4] 암호화 키를 생성합니다..." -ForegroundColor Yellow
    
    # Java 프로그램을 통해 암호화 키 생성
    $tempJavaFile = "$env:TEMP\GenerateKey.java"
    $javaCode = @"
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class GenerateKey {
    public static void main(String[] args) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            String base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
            System.out.println(base64Key);
        } catch (Exception e) {
            System.err.println("오류: " + e.getMessage());
            System.exit(1);
        }
    }
}
"@
    
    $javaCode | Out-File -FilePath $tempJavaFile -Encoding UTF8
    
    try {
        # Java 컴파일 및 실행
        $javaPath = (Get-Command java -ErrorAction SilentlyContinue).Source
        $javacPath = (Get-Command javac -ErrorAction SilentlyContinue).Source
        
        if (-not $javaPath -or -not $javacPath) {
            Write-Host "❌ Java가 설치되어 있지 않습니다." -ForegroundColor Red
            Write-Host "Java 17 이상을 설치하고 PATH에 추가하세요." -ForegroundColor Yellow
            exit 1
        }
        
        $tempDir = Split-Path -Parent $tempJavaFile
        $className = "GenerateKey"
        
        # 컴파일
        & $javacPath -d $tempDir $tempJavaFile
        if ($LASTEXITCODE -ne 0) {
            throw "Java 컴파일 실패"
        }
        
        # 실행하여 키 생성
        Push-Location $tempDir
        $EncryptionKey = & $javaPath $className | Select-Object -First 1
        Pop-Location
        
        Write-Host "✓ 암호화 키 생성 완료" -ForegroundColor Green
        Write-Host ""
    } catch {
        Write-Host "❌ 암호화 키 생성 실패: $_" -ForegroundColor Red
        exit 1
    } finally {
        # 임시 파일 정리
        if (Test-Path $tempJavaFile) {
            Remove-Item $tempJavaFile -Force
        }
        $classFile = Join-Path (Split-Path -Parent $tempJavaFile) "GenerateKey.class"
        if (Test-Path $classFile) {
            Remove-Item $classFile -Force
        }
    }
} else {
    Write-Host "[1/4] 사용자 제공 암호화 키 사용" -ForegroundColor Yellow
}

Write-Host "암호화 키 (ENCRYPTION_KEY):" -ForegroundColor Cyan
Write-Host $EncryptionKey -ForegroundColor White
Write-Host ""
Write-Host "⚠️ 이 암호화 키를 안전하게 보관하세요!" -ForegroundColor Yellow
Write-Host "서버의 .env 파일에 ENCRYPTION_KEY로 설정해야 합니다." -ForegroundColor Yellow
Write-Host ""

# 2. API 키 암호화
Write-Host "[2/4] API 키를 암호화합니다..." -ForegroundColor Yellow

$tempEncryptFile = "$env:TEMP\EncryptApiKey.java"
$encryptCode = @"
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptApiKey {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("사용법: java EncryptApiKey <plainText> <base64Key>");
            System.exit(1);
        }
        
        String plainText = args[0];
        String base64Key = args[1];
        
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "AES");
            
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String encryptedText = Base64.getEncoder().encodeToString(encryptedBytes);
            
            System.out.println(encryptedText);
        } catch (Exception e) {
            System.err.println("암호화 실패: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
"@

$encryptCode | Out-File -FilePath $tempEncryptFile -Encoding UTF8

try {
    $tempDir = Split-Path -Parent $tempEncryptFile
    $className = "EncryptApiKey"
    
    # 컴파일
    & $javacPath -d $tempDir $tempEncryptFile
    if ($LASTEXITCODE -ne 0) {
        throw "Java 컴파일 실패"
    }
    
    # 실행하여 암호화
    Push-Location $tempDir
    $EncryptedApiKey = & $javaPath $className $PlainApiKey $EncryptionKey | Select-Object -First 1
    Pop-Location
    
    Write-Host "✓ API 키 암호화 완료" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ API 키 암호화 실패: $_" -ForegroundColor Red
    exit 1
} finally {
    # 임시 파일 정리
    if (Test-Path $tempEncryptFile) {
        Remove-Item $tempEncryptFile -Force
    }
    $classFile = Join-Path (Split-Path -Parent $tempEncryptFile) "EncryptApiKey.class"
    if (Test-Path $classFile) {
        Remove-Item $classFile -Force
    }
}

# 3. 결과 출력
Write-Host "[3/4] 암호화 결과" -ForegroundColor Yellow
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "원본 API 키 (평문):" -ForegroundColor Yellow
Write-Host $PlainApiKey -ForegroundColor Gray
Write-Host ""
Write-Host "암호화된 API 키 (GEMINI_API_KEY):" -ForegroundColor Cyan
Write-Host $EncryptedApiKey -ForegroundColor White
Write-Host ""
Write-Host "암호화 키 (ENCRYPTION_KEY):" -ForegroundColor Cyan
Write-Host $EncryptionKey -ForegroundColor White
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# 4. 서버 .env 파일 설정 안내
Write-Host "[4/4] 서버 .env 파일 설정" -ForegroundColor Yellow
Write-Host ""
Write-Host "서버의 .env 파일에 다음 두 값을 추가하세요:" -ForegroundColor White
Write-Host ""
Write-Host "# 암호화 키 (복호화에 사용)" -ForegroundColor Gray
Write-Host "ENCRYPTION_KEY=$EncryptionKey" -ForegroundColor Green
Write-Host ""
Write-Host "# 암호화된 Gemini API 키" -ForegroundColor Gray
Write-Host "GEMINI_API_KEY=$EncryptedApiKey" -ForegroundColor Green
Write-Host ""

# 5. 결과를 파일로 저장 (선택사항)
$outputFile = "encrypted_api_key_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
$outputContent = @"
# Gemini API 키 암호화 결과
# 생성 시간: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

# 암호화 키 (ENCRYPTION_KEY)
ENCRYPTION_KEY=$EncryptionKey

# 암호화된 Gemini API 키 (GEMINI_API_KEY)
GEMINI_API_KEY=$EncryptedApiKey

# ==========================================
# 서버 .env 파일에 위 두 값을 추가하세요.
# ==========================================
"@

$outputContent | Out-File -FilePath $outputFile -Encoding UTF8
Write-Host "✓ 암호화 결과가 파일로 저장되었습니다: $outputFile" -ForegroundColor Green
Write-Host ""
Write-Host "⚠️ 이 파일을 안전하게 보관하고, Git에 커밋하지 마세요!" -ForegroundColor Yellow
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "암호화 완료!" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan


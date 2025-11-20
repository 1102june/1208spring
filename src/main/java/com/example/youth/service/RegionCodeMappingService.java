package com.example.youth.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 광역시도 및 시군구 코드 매핑 서비스
 * Excel 파일에서 지역명을 코드로 변환
 */
@Service
public class RegionCodeMappingService {
    
    private final ResourceLoader resourceLoader;
    
    // Excel 파일 경로 (우선순위: 1. classpath, 2. 프로젝트 루트, 3. 절대 경로)
    private static final String EXCEL_FILE_NAME = "광역시도_시군구_코드목록.xlsx";
    private static final String EXCEL_FILE_PATH_ABSOLUTE = "C:\\광역시도_시군구_코드목록.xlsx";
    
    public RegionCodeMappingService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    // 지역명 -> 광역시도 코드 매핑
    private Map<String, String> regionNameToBrtcCode = new HashMap<>();
    
    // 지역명 -> 시군구 코드 매핑 (광역시도명 + 시군구명 -> 시군구 코드)
    private Map<String, String> regionNameToSignguCode = new HashMap<>();
    
    // 광역시도 코드 -> 시군구 코드 목록 매핑
    private Map<String, Map<String, String>> brtcCodeToSignguCodes = new HashMap<>();
    
    /**
     * Excel 파일 경로 찾기 (우선순위: classpath > 프로젝트 루트 > 절대 경로)
     */
    private String findExcelFilePath() {
        // 1. classpath에서 찾기 (src/main/resources)
        try {
            Resource resource = new ClassPathResource(EXCEL_FILE_NAME);
            if (resource.exists()) {
                String path = resource.getFile().getAbsolutePath();
                System.out.println("✅ Excel 파일을 classpath에서 찾았습니다: " + path);
                return path;
            }
        } catch (Exception e) {
            // classpath에서 찾지 못함
        }
        
        // 2. 프로젝트 루트에서 찾기
        String projectRoot = System.getProperty("user.dir");
        File projectRootFile = new File(projectRoot, EXCEL_FILE_NAME);
        if (projectRootFile.exists() && projectRootFile.canRead()) {
            System.out.println("✅ Excel 파일을 프로젝트 루트에서 찾았습니다: " + projectRootFile.getAbsolutePath());
            return projectRootFile.getAbsolutePath();
        }
        
        // 3. 절대 경로에서 찾기
        File absoluteFile = new File(EXCEL_FILE_PATH_ABSOLUTE);
        if (absoluteFile.exists() && absoluteFile.canRead()) {
            System.out.println("✅ Excel 파일을 절대 경로에서 찾았습니다: " + absoluteFile.getAbsolutePath());
            return absoluteFile.getAbsolutePath();
        }
        
        return null;
    }
    
    /**
     * Excel 파일을 읽어서 매핑 데이터 초기화
     */
    public void initializeMapping() {
        // Excel 파일 경로 찾기
        String excelFilePath = findExcelFilePath();
        
        if (excelFilePath == null) {
            System.err.println("⚠️ Excel 파일을 찾을 수 없습니다.");
            System.err.println("   찾은 위치:");
            System.err.println("   1. classpath: " + EXCEL_FILE_NAME);
            System.err.println("   2. 프로젝트 루트: " + System.getProperty("user.dir") + File.separator + EXCEL_FILE_NAME);
            System.err.println("   3. 절대 경로: " + EXCEL_FILE_PATH_ABSOLUTE);
            System.err.println("   지역 코드 매핑이 작동하지 않습니다. Excel 파일을 위 위치 중 하나에 배치하세요.");
            return;
        }
        
        File excelFile = new File(excelFilePath);
        System.out.println("Excel 파일 읽기 시작: " + excelFilePath);
        System.out.println("파일 크기: " + excelFile.length() + " bytes");
        
        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용
            
            System.out.println("시트 이름: " + sheet.getSheetName());
            System.out.println("총 행 수: " + (sheet.getLastRowNum() + 1));
            
            // 첫 번째 행 확인 (헤더인지 데이터인지)
            Row firstRow = sheet.getRow(0);
            if (firstRow != null) {
                System.out.println("첫 번째 행 샘플 (처음 5개 셀):");
                for (int i = 0; i < 5 && i < firstRow.getLastCellNum(); i++) {
                    Cell cell = firstRow.getCell(i);
                    String value = getCellValueAsString(cell);
                    System.out.println("  Cell[" + i + "]: " + value);
                }
            }
            
            // 두 번째, 세 번째 행도 확인
            for (int sampleRow = 1; sampleRow <= 2 && sampleRow <= sheet.getLastRowNum(); sampleRow++) {
                Row row = sheet.getRow(sampleRow);
                if (row != null) {
                    System.out.println("데이터 행 " + sampleRow + " 샘플 (처음 5개 셀):");
                    for (int i = 0; i < 5 && i < row.getLastCellNum(); i++) {
                        Cell cell = row.getCell(i);
                        String value = getCellValueAsString(cell);
                        System.out.println("  Cell[" + i + "]: " + value);
                    }
                }
            }
            
            // 헤더 행 건너뛰기 (첫 번째 행이 헤더라고 가정)
            // 실제로는 첫 번째 행이 헤더, 두 번째 행부터 데이터
            int startRow = 1;
            int processedRows = 0;
            int skippedRows = 0;
            
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    skippedRows++;
                    continue;
                }
                
                // Excel 파일 구조 (실제 파일 기준):
                // Cell[0]: null (비어있음)
                // Cell[1]: 법정동코드 (10자리 숫자, 예: "1100000000")
                // Cell[2]: 법정동명 (예: "서울특별시", "서울특별시 종로구")
                // Cell[3]: 광역시도코드 (2자리 숫자, 예: "11")
                // Cell[4]: 시군구코드 (3자리 숫자, 예: "110" 또는 null)
                
                Cell cell0 = row.getCell(0); // null (비어있음)
                Cell cell1 = row.getCell(1); // 법정동코드 (10자리 숫자, 예: "1100000000")
                Cell cell2 = row.getCell(2); // 법정동명 (예: "서울특별시", "서울특별시 종로구")
                Cell cell3 = row.getCell(3); // 광역시도코드 (예: "11")
                Cell cell4 = row.getCell(4); // 시군구코드 (예: "110" 또는 null)
                
                // 법정동명(Cell[2])과 광역시도코드(Cell[3])는 필수
                if (cell2 == null || cell3 == null) {
                    skippedRows++;
                    continue;
                }
                
                String cell0Value = cell0 != null ? getCellValueAsString(cell0) : null; // null
                String cell1Value = cell1 != null ? getCellValueAsString(cell1) : null; // 법정동코드
                String regionName = getCellValueAsString(cell2); // 법정동명
                String brtcCode = getCellValueAsString(cell3); // 광역시도코드
                String signguCode = cell4 != null ? getCellValueAsString(cell4) : null; // 시군구코드
                
                // 디버깅: 처음 몇 개 행만 상세 로그 출력
                if (i <= 3) {
                    System.out.println("행 #" + i + " 디버깅: Cell[0]='" + cell0Value + "', Cell[1]='" + cell1Value + 
                                     "', Cell[2]='" + regionName + "', Cell[3]='" + brtcCode + "', Cell[4]='" + signguCode + "'");
                }
                
                // 법정동명과 광역시도코드는 필수, 시군구코드는 옵션
                if (regionName == null || brtcCode == null) {
                    skippedRows++;
                    continue;
                }
                
                // 헤더 행 건너뛰기
                // Cell[1]이 "법정동코드"이거나, Cell[2]가 "법정동명"이거나, Cell[3]이 "광역시도코드"이면 헤더
                boolean isHeaderRow = false;
                if (cell1Value != null && cell1Value.equals("법정동코드")) {
                    isHeaderRow = true;
                }
                if (regionName != null && regionName.equals("법정동명")) {
                    isHeaderRow = true;
                }
                if (brtcCode != null && (brtcCode.equals("광역시도코드") || brtcCode.equals("시군구코드"))) {
                    isHeaderRow = true;
                }
                
                // 법정동코드(Cell[1])가 숫자로만 이루어져 있고, 법정동명(Cell[2])도 숫자면 헤더로 간주하지 않음
                // (실제 데이터도 법정동코드가 숫자일 수 있음)
                
                if (isHeaderRow) {
                    skippedRows++;
                    if (i <= 3) {
                        System.out.println("  -> 헤더 행으로 판단하여 건너뜀");
                    }
                    continue;
                }
                
                // 지역명 파싱
                // Excel 파일 구조:
                // - "서울특별시" (광역시도만)
                // - "서울특별시 종로구" (광역시도 + 시군구)
                // - "경기도 수원시" (광역시도 + 시)
                // - "경기도 수원시 장안구" (광역시도 + 시 + 구)
                
                String[] parts = regionName.split("\\s+");
                
                if (parts.length >= 1) {
                    // 최소한 광역시도명은 추출
                    String brtcName = parts[0]; // 광역시도명 (예: "서울특별시", "경기도")
                    
                    // 광역시도명 -> 광역시도 코드 매핑 (중복 제거, 같은 이름이면 첫 번째 것만 사용)
                    if (!regionNameToBrtcCode.containsKey(brtcName)) {
                        regionNameToBrtcCode.put(brtcName, brtcCode);
                    }
                    
                    // 시군구 정보가 있는 경우 (parts.length >= 2) 그리고 시군구 코드가 있는 경우
                    if (parts.length >= 2 && signguCode != null && !signguCode.isEmpty()) {
                        // "서울특별시 종로구" -> "종로구"
                        // "경기도 수원시" -> "수원시"
                        // "경기도 수원시 장안구" -> "수원시 장안구" (시 + 구)
                        StringBuilder signguNameBuilder = new StringBuilder();
                        for (int j = 1; j < parts.length; j++) {
                            if (signguNameBuilder.length() > 0) {
                                signguNameBuilder.append(" ");
                            }
                            signguNameBuilder.append(parts[j]);
                        }
                        String signguName = signguNameBuilder.toString();
                        
                        // 광역시도명 + 시군구명 -> 시군구 코드 매핑
                        String key = brtcName + " " + signguName;
                        regionNameToSignguCode.put(key, signguCode);
                        
                        // 광역시도 코드별 시군구 코드 목록 저장
                        // 시군구명은 "수원시" 또는 "수원시 장안구" 형식으로 저장
                        brtcCodeToSignguCodes.computeIfAbsent(brtcCode, k -> new HashMap<>())
                                .put(signguName, signguCode);
                    }
                    
                    processedRows++;
                } else {
                    skippedRows++;
                }
                
                // 처음 10개 행만 상세 로그 출력
                if (processedRows <= 10) {
                    System.out.println("처리된 행 #" + processedRows + ": regionName='" + regionName + 
                                     "', brtcCode='" + brtcCode + "', signguCode='" + signguCode + "'");
                }
            }
            
            System.out.println("========================================");
            System.out.println("지역 코드 매핑 초기화 완료:");
            System.out.println("  - 처리된 행: " + processedRows + "개");
            System.out.println("  - 건너뛴 행: " + skippedRows + "개");
            System.out.println("  - 광역시도 매핑: " + regionNameToBrtcCode.size() + "개");
            System.out.println("  - 시군구 매핑: " + regionNameToSignguCode.size() + "개");
            System.out.println("  - 광역시도별 시군구 코드 그룹: " + brtcCodeToSignguCodes.size() + "개");
            System.out.println("========================================");
            
            // 매핑 샘플 출력 (처음 10개)
            if (!regionNameToBrtcCode.isEmpty()) {
                System.out.println("광역시도 매핑 샘플 (처음 10개):");
                int count = 0;
                for (Map.Entry<String, String> entry : regionNameToBrtcCode.entrySet()) {
                    if (count++ >= 10) break;
                    System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
                }
            }
            
            // 광역시도별 시군구 코드 샘플 출력
            if (!brtcCodeToSignguCodes.isEmpty()) {
                System.out.println("광역시도별 시군구 코드 샘플 (처음 5개):");
                int count = 0;
                for (Map.Entry<String, Map<String, String>> entry : brtcCodeToSignguCodes.entrySet()) {
                    if (count++ >= 5) break;
                    System.out.println("  광역시도 코드 " + entry.getKey() + ": " + entry.getValue().size() + "개 시군구");
                    int signguCount = 0;
                    for (Map.Entry<String, String> signguEntry : entry.getValue().entrySet()) {
                        if (signguCount++ >= 3) break;
                        System.out.println("    - " + signguEntry.getKey() + " -> " + signguEntry.getValue());
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Excel 파일 읽기 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cell 값을 String으로 변환
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 숫자를 문자열로 변환 (소수점 제거)
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * 지역명으로 광역시도 코드 조회
     * @param regionName 지역명 (예: "서울특별시", "충청북도")
     * @return 광역시도 코드 (예: "11", "43")
     */
    public String getBrtcCode(String regionName) {
        if (regionName == null || regionName.isEmpty()) return null;
        
        // 매핑이 비어있으면 초기화
        if (regionNameToBrtcCode.isEmpty()) {
            initializeMapping();
        }
        
        // 정확한 매칭 시도
        String code = regionNameToBrtcCode.get(regionName);
        if (code != null) return code;
        
        // 부분 매칭 시도 (예: "충청북도" -> "충북")
        for (Map.Entry<String, String> entry : regionNameToBrtcCode.entrySet()) {
            if (entry.getKey().contains(regionName) || regionName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * 지역명으로 시군구 코드 조회
     * @param brtcName 광역시도명 (예: "서울특별시")
     * @param signguName 시군구명 (예: "종로구")
     * @return 시군구 코드 (예: "110")
     */
    public String getSignguCode(String brtcName, String signguName) {
        if (brtcName == null || signguName == null) return null;
        
        // 매핑이 비어있으면 초기화
        if (regionNameToSignguCode.isEmpty()) {
            initializeMapping();
        }
        
        String key = brtcName + " " + signguName;
        return regionNameToSignguCode.get(key);
    }
    
    /**
     * 광역시도 코드로 해당 지역의 모든 시군구 코드 목록 조회
     * @param brtcCode 광역시도 코드
     * @return 시군구명 -> 시군구 코드 매핑
     */
    public Map<String, String> getSignguCodesByBrtcCode(String brtcCode) {
        if (brtcCode == null) return new HashMap<>();
        
        // 매핑이 비어있으면 초기화
        if (brtcCodeToSignguCodes.isEmpty()) {
            initializeMapping();
        }
        
        return brtcCodeToSignguCodes.getOrDefault(brtcCode, new HashMap<>());
    }
    
    /**
     * 공고문 API의 CNP_CD_NM (지역명)을 광역시도 코드로 변환
     * @param cnpCdNm 지역명 (예: "충청북도", "서울특별시")
     * @return 광역시도 코드 (예: "43", "11")
     */
    public String convertCnpCdNmToBrtcCode(String cnpCdNm) {
        if (cnpCdNm == null || cnpCdNm.isEmpty()) return null;
        
        // 매핑이 비어있으면 초기화
        if (regionNameToBrtcCode.isEmpty()) {
            initializeMapping();
        }
        
        // "전국", "외" 같은 특수 케이스 처리
        String cleanCnpCdNm = cnpCdNm.trim();
        if (cleanCnpCdNm.contains("전국") || cleanCnpCdNm.endsWith(" 외")) {
            return null; // 전국은 특별 처리 필요
        }
        
        // "외" 제거 (예: "대구광역시 외" -> "대구광역시")
        if (cleanCnpCdNm.endsWith(" 외")) {
            cleanCnpCdNm = cleanCnpCdNm.substring(0, cleanCnpCdNm.length() - 2).trim();
        }
        
        // 1. 정확한 매칭 시도
        String code = regionNameToBrtcCode.get(cleanCnpCdNm);
        if (code != null) {
            return code;
        }
        
        // 2. 공백 제거 후 매칭 시도
        String noSpaceCnpCdNm = cleanCnpCdNm.replaceAll("\\s+", "");
        for (Map.Entry<String, String> entry : regionNameToBrtcCode.entrySet()) {
            String key = entry.getKey();
            String noSpaceKey = key.replaceAll("\\s+", "");
            if (noSpaceKey.equals(noSpaceCnpCdNm)) {
                return entry.getValue();
            }
        }
        
        // 3. 부분 매칭 시도 (양방향)
        for (Map.Entry<String, String> entry : regionNameToBrtcCode.entrySet()) {
            String key = entry.getKey();
            // Excel의 지역명이 공고문 지역명을 포함하거나, 그 반대인 경우
            if (key.equals(cleanCnpCdNm) || 
                key.contains(cleanCnpCdNm) || 
                cleanCnpCdNm.contains(key)) {
                return entry.getValue();
            }
        }
        
        // 4. 특수 문자 제거 후 매칭 시도 (예: "강원특별자치도" vs "강원도")
        String normalizedCnpCdNm = cleanCnpCdNm
                .replace("특별시", "")
                .replace("광역시", "")
                .replace("특별자치시", "")
                .replace("특별자치도", "")
                .replace("도", "")
                .trim();
        
        for (Map.Entry<String, String> entry : regionNameToBrtcCode.entrySet()) {
            String key = entry.getKey();
            String normalizedKey = key
                    .replace("특별시", "")
                    .replace("광역시", "")
                    .replace("특별자치시", "")
                    .replace("특별자치도", "")
                    .replace("도", "")
                    .trim();
            
            if (normalizedKey.equals(normalizedCnpCdNm) && !normalizedKey.isEmpty()) {
                return entry.getValue();
            }
        }
        
        // 디버깅: 매핑에 없는 지역명 출력 (첫 번째만 상세 출력)
        if (regionNameToBrtcCode.size() > 0) {
            // 첫 번째 매칭 실패만 상세 로그 출력
            if (!regionNameToBrtcCode.containsKey("_first_fail_logged")) {
                System.out.println("⚠️ 매핑에 없는 지역명: '" + cnpCdNm + "' (정리 후: '" + cleanCnpCdNm + "')");
                System.out.println("   현재 매핑 키 목록: " + regionNameToBrtcCode.keySet());
                regionNameToBrtcCode.put("_first_fail_logged", ""); // 플래그
            }
        }
        
        return null;
    }
}


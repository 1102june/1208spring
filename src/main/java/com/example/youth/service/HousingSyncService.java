package com.example.youth.service;

import com.example.youth.DB.Housing;
import com.example.youth.dto.publicdata.LHRentalHouseListResponse;
import com.example.youth.repository.HousingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HousingSyncService {

    @Autowired
    private PublicDataApiService publicDataApiService;

    @Autowired
    private HousingRepository housingRepository; // 사용됨: convertToHousing, updateHousingData에서 사용

    /**
     * LH 공공데이터에서 임대주택 정보를 가져와 DB에 동기화
     */
    @Transactional
    public void syncLHRentalHouseData(String brtcCode, String signguCode) {
        publicDataApiService.getAllLHRentalHouseList(brtcCode, signguCode)
                .subscribe(
                        response -> {
                            if (response.getResponse() != null 
                                    && response.getResponse().getBody() != null
                                    && response.getResponse().getBody().getItems() != null
                                    && response.getResponse().getBody().getItems().getItem() != null) {
                                
                                List<LHRentalHouseListResponse.Item> items = 
                                        response.getResponse().getBody().getItems().getItem();
                                
                                List<Housing> housingList = items.stream()
                                        .map(this::convertToHousing)
                                        .collect(Collectors.toList());
                                
                                // DB에 저장 (이미 존재하면 업데이트)
                                for (Housing housing : housingList) {
                                    housingRepository.findById(housing.getHousingId())
                                            .ifPresentOrElse(
                                                    existing -> {
                                                        // 기존 데이터 업데이트
                                                        updateHousingData(existing, housing);
                                                        housingRepository.save(existing);
                                                    },
                                                    () -> {
                                                        // 새 데이터 저장
                                                        housingRepository.save(housing);
                                                    }
                                            );
                                }
                                
                                System.out.println("임대주택 데이터 동기화 완료: " + housingList.size() + "건");
                            }
                        },
                        error -> {
                            System.err.println("임대주택 데이터 동기화 실패: " + error.getMessage());
                            error.printStackTrace();
                        }
                );
    }

    /**
     * LH API 응답 Item을 Housing 엔티티로 변환
     */
    private Housing convertToHousing(LHRentalHouseListResponse.Item item) {
        // 단지 식별자를 housingId로 사용
        String housingId = item.getHsmpSn() != null ? item.getHsmpSn() : 
                generateHousingId(item.getBrtcCode(), item.getSignguCode(), item.getHsmpNm());

        Housing.HousingBuilder builder = Housing.builder()
                .housingId(housingId)
                .name(item.getHsmpNm() != null ? item.getHsmpNm() : "")
                .address(item.getRnAdres() != null ? item.getRnAdres() : "")
                .organization(item.getInsttNm() != null ? item.getInsttNm() : "")
                .heatingType(item.getHeatMthdDetailNm() != null ? item.getHeatMthdDetailNm() : "")
                .elevator(item.getElvtrInstlAtNm() != null && 
                         (item.getElvtrInstlAtNm().contains("설치") || 
                          item.getElvtrInstlAtNm().contains("전체")))
                .link("https://www.myhome.go.kr/hws/portal/cont/selectContRentalView.do#guide=" + item.getHsmpSn());

        // 공급 면적 파싱
        if (item.getSuplyPrvuseAr() != null && !item.getSuplyPrvuseAr().isEmpty()) {
            try {
                builder.supplyArea(Double.parseDouble(item.getSuplyPrvuseAr()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 준공 일자 파싱
        if (item.getCompetDe() != null && !item.getCompetDe().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                java.util.Date date = sdf.parse(item.getCompetDe());
                builder.completeDate(new Date(date.getTime()));
            } catch (ParseException e) {
                // 파싱 실패 시 무시
            }
        }

        // 주차수 파싱
        if (item.getParkngCo() != null && !item.getParkngCo().isEmpty()) {
            try {
                builder.parkingSpaces(Integer.parseInt(item.getParkngCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 세대수 파싱
        if (item.getHshldCo() != null && !item.getHshldCo().isEmpty()) {
            try {
                builder.totalUnits(Integer.parseInt(item.getHshldCo()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 기본 임대보증금 파싱
        if (item.getBassRentGtn() != null && !item.getBassRentGtn().isEmpty()) {
            try {
                builder.deposit(Integer.parseInt(item.getBassRentGtn()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 기본 월임대료 파싱
        if (item.getBassMtRntchrg() != null && !item.getBassMtRntchrg().isEmpty()) {
            try {
                builder.monthlyRent(Integer.parseInt(item.getBassMtRntchrg()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // 주택유형 설정
        if (item.getSuplyTyNm() != null && !item.getSuplyTyNm().isEmpty()) {
            builder.housingType(item.getSuplyTyNm());
        }

        // 기본 전환보증금을 보증금환급금으로 사용
        if (item.getBassCnvrsGtnLmt() != null && !item.getBassCnvrsGtnLmt().isEmpty()) {
            try {
                builder.depositRefund(Integer.parseInt(item.getBassCnvrsGtnLmt()));
            } catch (NumberFormatException e) {
                // 파싱 실패 시 무시
            }
        }

        // TODO: 주소를 기반으로 좌표 조회 (카카오맵 API 또는 다른 지오코딩 서비스 사용)
        // 현재는 null로 설정, 추후 카카오맵 API로 주소 -> 좌표 변환 필요
        builder.latitude(null);
        builder.longitude(null);

        return builder.build();
    }

    /**
     * Housing ID 생성 (hsmpSn이 없는 경우)
     */
    private String generateHousingId(String brtcCode, String signguCode, String hsmpNm) {
        return (brtcCode != null ? brtcCode : "") + 
               (signguCode != null ? signguCode : "") + 
               (hsmpNm != null ? hsmpNm.hashCode() : System.currentTimeMillis());
    }

    /**
     * 기존 Housing 데이터 업데이트
     */
    private void updateHousingData(Housing existing, Housing newData) {
        existing.setName(newData.getName());
        existing.setAddress(newData.getAddress());
        existing.setSupplyArea(newData.getSupplyArea());
        existing.setCompleteDate(newData.getCompleteDate());
        existing.setOrganization(newData.getOrganization());
        existing.setHeatingType(newData.getHeatingType());
        existing.setElevator(newData.getElevator());
        existing.setParkingSpaces(newData.getParkingSpaces());
        existing.setDeposit(newData.getDeposit());
        existing.setMonthlyRent(newData.getMonthlyRent());
        existing.setTotalUnits(newData.getTotalUnits());
        existing.setLink(newData.getLink());
        existing.setHousingType(newData.getHousingType());
        existing.setDepositRefund(newData.getDepositRefund());
        // 좌표는 주소가 변경된 경우에만 업데이트
        if (newData.getLatitude() != null && newData.getLongitude() != null) {
            existing.setLatitude(newData.getLatitude());
            existing.setLongitude(newData.getLongitude());
        }
    }
}


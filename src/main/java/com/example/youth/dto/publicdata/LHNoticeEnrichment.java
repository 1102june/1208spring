package com.example.youth.dto.publicdata;

import lombok.Data;

/**
 * 분양임대공고별 상세정보(lhLeaseNoticeDtlInfo1) + 공급정보(lhLeaseNoticeSplInfo1)
 * API 응답에서 추출한 공고 보강(enrichment) 데이터.
 */
@Data
public class LHNoticeEnrichment {
    private String rnAdres;     // 단지 주소 (dsSbd.LCT_ARA_ADR)
    private String scAr;        // 전용면적 범위 (dsSbd.SC_AR)
    private String hshldCo;     // 총세대수 (dsSbd.HSH_CNT)
    private String htnFmlaNm;   // 난방방식 (dsSbd.HTN_FMLA_DS_CD_NM)
    private String mvinXpcYm;   // 입주예정월 (dsSbd.MVIN_XPC_YM)
    private String panDtlCts;   // 공고 상세내용 (dsCtrtPlc.PAN_DTL_CTS)
    private String splXpcAmt;   // 대표 공급금액/예정가격 (dsList01.SPL_XPC_AMT)

    /** 상세/공급 API 중 하나라도 유효 데이터가 채워졌는지 여부 */
    public boolean hasAnyData() {
        return rnAdres != null || scAr != null || hshldCo != null
                || htnFmlaNm != null || mvinXpcYm != null
                || panDtlCts != null || splXpcAmt != null;
    }
}

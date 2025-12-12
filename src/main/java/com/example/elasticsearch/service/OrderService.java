package com.example.elasticsearch.service;

import com.example.elasticsearch.dto.OrderInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Oracle DB 주문 정보 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final JdbcTemplate jdbcTemplate;

    // 주문상태 코드 매핑 (OC21)
    private static final Map<String, String> ORDER_STATUS_MAP = new HashMap<>() {{
        put("110", "입금대기");
        put("120", "주문완료");
        put("130", "배송지시");
        put("140", "피킹완료");
        put("145", "입고지연");
        put("146", "결품요청");
        put("150", "패킹완료");
        put("160", "출하완료");
        put("170", "배송완료");
        put("180", "주문취소");
        // 반품상태
        put("210", "반품요청");
        put("220", "반품접수");
        put("230", "수거지시");
        put("235", "수거지시확인");
        put("240", "회수확인");
        put("246", "판정대기");
        put("248", "판정완료");
        put("250", "입고완료");
        put("260", "환불대기");
        put("270", "환불완료");
        put("299", "반품철회");
        // 교환상태
        put("310", "교환요청");
        put("320", "교환접수");
        put("330", "수거지시");
        put("335", "수거지시확인");
        put("336", "판정대기");
        put("338", "판정완료");
        put("340", "입고완료");
        put("350", "출하지시");
        put("360", "피킹완료");
        put("370", "패킹완료");
        put("380", "출하완료");
        put("390", "배송완료");
        put("399", "교환철회");
    }};

    // 배송방법 코드 매핑 (PO21)
    private static final Map<String, String> SHIPPING_METHOD_MAP = new HashMap<>() {{
        put("10", "자사배송");
        put("20", "택배배송");
        put("30", "매장방문");
        put("40", "등기");
        put("50", "미배송");
        put("60", "미발송");
        put("70", "퀵/당일배송");
        put("80", "글로벌배송");
        put("90", "특수배송");
    }};

    /**
     * 주문번호 + 상품순번으로 주문 정보 조회
     */
    public OrderInfo getOrderInfo(String ordNo, Integer ordItemSeq) {
        log.info("🔍 주문 정보 조회: ordNo={}, ordItemSeq={}", ordNo, ordItemSeq);

        String sql = """
            SELECT 
                ORD_NO,
                ORD_ITEM_SEQ,
                ORD_ITEM_STAT_CD,
                ITEM_ID,
                ITEM_NM,
                UITEM_ID,
                UITEM_NM,
                ORD_QTY,
                CNCL_QTY,
                RET_QTY,
                ORD_AMT,
                DC_AMT,
                RLORD_AMT,
                SHPP_MTHD_CD,
                SHPP_RSVT_DT,
                SHPP_DIRC_EXPC_DT,
                CLM_RSN_CD,
                CLM_RSN_CNTT,
                TO_CHAR(ORD_RCP_DTS, 'YYYY-MM-DD HH24:MI:SS') AS ORD_RCP_DTS,
                TO_CHAR(ORD_ITEM_STAT_CHNG_DTS, 'YYYY-MM-DD HH24:MI:SS') AS ORD_ITEM_STAT_CHNG_DTS
            FROM SSG.ORD_ITEM
            WHERE ORD_NO = ?
              AND ORD_ITEM_SEQ = ?
            """;

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                String statCd = rs.getString("ORD_ITEM_STAT_CD");
                String shppCd = rs.getString("SHPP_MTHD_CD");
                String clmCd = rs.getString("CLM_RSN_CD");

                return OrderInfo.builder()
                        .ordNo(rs.getString("ORD_NO"))
                        .ordItemSeq(rs.getInt("ORD_ITEM_SEQ"))
                        .ordItemStatCd(statCd)
                        .ordItemStatNm(ORDER_STATUS_MAP.getOrDefault(statCd, statCd))
                        .itemId(rs.getString("ITEM_ID"))
                        .itemNm(rs.getString("ITEM_NM"))
                        .uitemId(rs.getString("UITEM_ID"))
                        .uitemNm(rs.getString("UITEM_NM"))
                        .ordQty(rs.getInt("ORD_QTY"))
                        .cnclQty(rs.getInt("CNCL_QTY"))
                        .retQty(rs.getInt("RET_QTY"))
                        .ordAmt(rs.getLong("ORD_AMT"))
                        .dcAmt(rs.getLong("DC_AMT"))
                        .rlordAmt(rs.getLong("RLORD_AMT"))
                        .shppMthdCd(shppCd)
                        .shppMthdNm(SHIPPING_METHOD_MAP.getOrDefault(shppCd, shppCd))
                        .shppRsvtDt(rs.getString("SHPP_RSVT_DT"))
                        .shppDircExpcDt(rs.getString("SHPP_DIRC_EXPC_DT"))
                        .clmRsnCd(clmCd)
                        .clmRsnNm(getClaimReasonName(clmCd))
                        .clmRsnCntt(rs.getString("CLM_RSN_CNTT"))
                        .ordRcpDts(rs.getString("ORD_RCP_DTS"))
                        .ordItemStatChngDts(rs.getString("ORD_ITEM_STAT_CHNG_DTS"))
                        .build();
            }, ordNo, ordItemSeq);

        } catch (Exception e) {
            log.warn("⚠️ 주문 정보 조회 실패: ordNo={}, ordItemSeq={}, error={}", 
                    ordNo, ordItemSeq, e.getMessage());
            return null;
        }
    }

    /**
     * 클레임 사유 코드 → 사유명 변환 (DB 조회)
     */
    private String getClaimReasonName(String clmRsnCd) {
        if (clmRsnCd == null || clmRsnCd.isEmpty()) {
            return null;
        }

        try {
            String sql = "SELECT COMM_CD_NM FROM SSG.COMM_CD_DTLC WHERE COMM_CD_GRP_NO = 'OR07' AND COMM_CD_NO = ?";
            return jdbcTemplate.queryForObject(sql, String.class, clmRsnCd);
        } catch (Exception e) {
            log.debug("클레임 사유명 조회 실패: {}", clmRsnCd);
            return clmRsnCd;
        }
    }

    /**
     * 주문 상태에 따른 가능한 액션 안내
     */
    public String getAvailableActions(String ordItemStatCd) {
        if (ordItemStatCd == null) return "";

        return switch (ordItemStatCd) {
            case "110" -> "입금대기 상태입니다. 결제 완료 후 주문이 진행됩니다. 취소가 가능합니다.";
            case "120" -> "주문완료 상태입니다. 출고 전 취소가 가능합니다.";
            case "130", "140", "145", "146", "150" -> "배송준비중 상태입니다. 출고 전 취소 요청이 가능합니다.";
            case "160" -> "출하완료 상태입니다. 배송중이므로 취소가 어렵습니다. 수령 후 반품 신청해주세요.";
            case "170" -> "배송완료 상태입니다. 반품/교환 신청이 가능합니다 (배송완료 후 7일 이내).";
            case "180" -> "주문취소 완료된 상태입니다.";
            // 반품
            case "210", "220" -> "반품요청/접수 상태입니다. 수거 예정입니다.";
            case "230", "235", "240" -> "반품 수거 진행중입니다.";
            case "246", "248", "250" -> "반품 입고 및 검수 진행중입니다.";
            case "260" -> "환불대기 상태입니다. 곧 환불 처리됩니다.";
            case "270" -> "환불완료 상태입니다.";
            case "299" -> "반품이 철회되었습니다.";
            // 교환
            case "310", "320" -> "교환요청/접수 상태입니다. 수거 예정입니다.";
            case "330", "335", "340" -> "교환 수거 및 입고 진행중입니다.";
            case "350", "360", "370", "380" -> "교환상품 출고 진행중입니다.";
            case "390" -> "교환상품 배송완료 상태입니다.";
            case "399" -> "교환이 철회되었습니다.";
            default -> "";
        };
    }
}


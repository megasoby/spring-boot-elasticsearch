package com.example.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 주문 정보 DTO (Oracle ORD_ITEM 테이블)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {
    
    // 주문 기본 정보
    private String ordNo;               // 주문번호
    private Integer ordItemSeq;         // 주문상품순번
    private String ordItemStatCd;       // 주문상품상태코드
    private String ordItemStatNm;       // 주문상품상태명 (코드 변환)
    
    // 상품 정보
    private String itemId;              // 상품ID
    private String itemNm;              // 상품명
    private String uitemId;             // 단위상품ID
    private String uitemNm;             // 단위상품명
    
    // 수량/금액
    private Integer ordQty;             // 주문수량
    private Integer cnclQty;            // 취소수량
    private Integer retQty;             // 반품수량
    private Long ordAmt;                // 주문금액
    private Long dcAmt;                 // 할인금액
    private Long rlordAmt;              // 실주문금액
    
    // 배송 정보
    private String shppMthdCd;          // 배송방법코드
    private String shppMthdNm;          // 배송방법명 (코드 변환)
    private String shppRsvtDt;          // 배송예약일
    private String shppDircExpcDt;      // 배송예정일
    
    // 클레임 정보
    private String clmRsnCd;            // 클레임사유코드
    private String clmRsnNm;            // 클레임사유명 (코드 변환)
    private String clmRsnCntt;          // 클레임사유내용
    
    // 일시 정보
    private String ordRcpDts;           // 주문접수일시
    private String ordItemStatChngDts;  // 상태변경일시
    
    /**
     * 상담용 요약 정보 생성
     */
    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 주문 정보 ===\n");
        sb.append(String.format("- 주문번호: %s (상품순번: %d)\n", ordNo, ordItemSeq));
        sb.append(String.format("- 상품명: %s\n", itemNm != null ? itemNm : "N/A"));
        sb.append(String.format("- 주문상태: %s (%s)\n", ordItemStatNm != null ? ordItemStatNm : "N/A", ordItemStatCd));
        sb.append(String.format("- 배송방법: %s\n", shppMthdNm != null ? shppMthdNm : "N/A"));
        sb.append(String.format("- 주문수량: %d개\n", ordQty != null ? ordQty : 0));
        sb.append(String.format("- 주문금액: %,d원\n", ordAmt != null ? ordAmt : 0));
        sb.append(String.format("- 할인금액: %,d원\n", dcAmt != null ? dcAmt : 0));
        sb.append(String.format("- 실결제금액: %,d원\n", rlordAmt != null ? rlordAmt : 0));
        
        if (cnclQty != null && cnclQty > 0) {
            sb.append(String.format("- 취소수량: %d개\n", cnclQty));
        }
        if (retQty != null && retQty > 0) {
            sb.append(String.format("- 반품수량: %d개\n", retQty));
        }
        if (clmRsnNm != null) {
            sb.append(String.format("- 클레임사유: %s\n", clmRsnNm));
        }
        if (clmRsnCntt != null && !clmRsnCntt.isEmpty()) {
            sb.append(String.format("- 클레임내용: %s\n", clmRsnCntt));
        }
        
        return sb.toString();
    }
}


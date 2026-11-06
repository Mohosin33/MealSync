package com.example.mealsync.finance;

public class EngineAdjustment {
    private final String id;
    private final String type; // "reversal" or "member_adjustment"
    private final String targetType; // "expense" or "deposit"
    private final String targetId;
    private final String memberId;
    private final long amountPaisa; // signed: positive = credit, negative = debit

    public static EngineAdjustment createReversal(String id, String targetType, String targetId) {
        return new EngineAdjustment(id, "reversal", targetType, targetId, null, 0);
    }

    public static EngineAdjustment createMemberAdjustment(String id, String memberId, long amountPaisa) {
        return new EngineAdjustment(id, "member_adjustment", null, null, memberId, amountPaisa);
    }

    public EngineAdjustment(String id, String type, String targetType, String targetId, String memberId, long amountPaisa) {
        this.id = id;
        this.type = type;
        this.targetType = targetType;
        this.targetId = targetId;
        this.memberId = memberId;
        this.amountPaisa = amountPaisa;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getMemberId() {
        return memberId;
    }

    public long getAmountPaisa() {
        return amountPaisa;
    }
}

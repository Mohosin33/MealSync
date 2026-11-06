package com.example.mealsync.finance;

public class EngineDeposit {
    private final String id;
    private final String memberId;
    private final long amountPaisa;

    public EngineDeposit(String id, String memberId, long amountPaisa) {
        this.id = id;
        this.memberId = memberId;
        this.amountPaisa = amountPaisa;
    }

    public String getId() {
        return id;
    }

    public String getMemberId() {
        return memberId;
    }

    public long getAmountPaisa() {
        return amountPaisa;
    }
}

package com.example.mealsync.finance;

public class EngineExpense {
    private final String id;
    private final long amountPaisa;
    private final String type; // Legacy variable expenses are ignored by the finance engine.

    public EngineExpense(String id, long amountPaisa, String type) {
        this.id = id;
        this.amountPaisa = amountPaisa;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public long getAmountPaisa() {
        return amountPaisa;
    }

    public String getType() {
        return type;
    }
}

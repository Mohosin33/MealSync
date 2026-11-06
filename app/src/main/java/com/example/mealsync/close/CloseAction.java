package com.example.mealsync.close;

public class CloseAction {
    private final CloseStep step;
    private final String closeRequestId;
    private final boolean alreadyDone;

    public CloseAction(CloseStep step, String closeRequestId, boolean alreadyDone) {
        this.step = step;
        this.closeRequestId = closeRequestId;
        this.alreadyDone = alreadyDone;
    }

    public CloseStep getStep() {
        return step;
    }

    public String getCloseRequestId() {
        return closeRequestId;
    }

    public boolean isAlreadyDone() {
        return alreadyDone;
    }
}

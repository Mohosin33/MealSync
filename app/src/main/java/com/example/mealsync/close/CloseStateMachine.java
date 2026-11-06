package com.example.mealsync.close;

public final class CloseStateMachine {

    private CloseStateMachine() {
    }

    public static CloseAction nextAction(String periodStatus, String storedCloseRequestId) {
        if ("closed".equalsIgnoreCase(periodStatus)) {
            return new CloseAction(CloseStep.DONE, storedCloseRequestId, true);
        } else if ("closing".equalsIgnoreCase(periodStatus)) {
            return new CloseAction(CloseStep.CALCULATE, storedCloseRequestId, false);
        } else {
            // OPEN
            return new CloseAction(CloseStep.LOCK, null, false);
        }
    }
}

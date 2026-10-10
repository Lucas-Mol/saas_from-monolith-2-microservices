package dev.agenda.legacy.appointments;

import jakarta.validation.constraints.Size;

public class CancelRequest {

    @Size(max = 500)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

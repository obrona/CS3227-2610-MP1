package jobtracker.model;

public enum ApplicationStatus {
    IN_PROGRESS("In progress"),
    OFFERED("Offered"),
    ACCEPTED("Accepted"),
    DECLINED("Declined"),
    REJECTED("Rejected");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

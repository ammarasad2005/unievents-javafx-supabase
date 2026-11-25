package unievents.shared;

/**
 * Audience breakdown returned by Event.getAudienceCounts() in SD-08.
 */
public record AudienceCounts(
        int registeredCount,
        int waitlistedCount,
        int bothCount
) {
    public int getRegisteredCount() { return registeredCount; }
    public int getWaitlistedCount() { return waitlistedCount; }
    public int getBothCount() { return bothCount; }
}

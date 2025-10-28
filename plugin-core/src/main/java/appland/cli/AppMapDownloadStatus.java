package appland.cli;

public enum AppMapDownloadStatus {
    /**
     * The download was successful.
     */
    Successful,
    /**
     * The download failed with an error.
     */
    Failed,
    /**
     * The download was skipped, e.g. because the DeploymentSettings prohibited it.
     */
    Skipped;

    public boolean isSuccessful() {
        return Successful.equals(this);
    }
}

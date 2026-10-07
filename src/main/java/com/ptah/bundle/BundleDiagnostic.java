package com.ptah.bundle;

public record BundleDiagnostic(Severity severity, String source, String message) {
    public enum Severity { INFO, WARNING, ERROR }
    public static BundleDiagnostic warning(String source, String message) { return new BundleDiagnostic(Severity.WARNING, source, message); }
    public static BundleDiagnostic error(String source, String message) { return new BundleDiagnostic(Severity.ERROR, source, message); }
}

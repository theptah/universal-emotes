package com.ptah.bundle;

import java.util.List;
import java.util.Optional;

public record BundleLoadResult(Optional<BundleDescriptor> bundle, List<BundleDiagnostic> diagnostics) {
    public BundleLoadResult { diagnostics = List.copyOf(diagnostics); }
}

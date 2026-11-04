package ru.d2.elysium.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class ConfigPrivacyTest {
    @Test void aFreshInstallDoesNotPublishPlayerStatistics() {
        assertFalse(new Config().syncWebsite);
    }
}

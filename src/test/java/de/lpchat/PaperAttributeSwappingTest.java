package de.lpchat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaperAttributeSwappingTest {
    public static final class Configuration {
        public Settings unsupportedSettings = new Settings();
    }

    public static final class Settings {
        public boolean updateEquipmentOnPlayerActions = true;
        public boolean allowPistonDuplication = false;
    }

    @Test void enablesSwappingAndRestoresOriginalSettingOnDisable() throws Exception {
        Configuration config = new Configuration();
        try (var override = PaperAttributeSwapping.enable(config)) {
            assertFalse(config.unsupportedSettings.updateEquipmentOnPlayerActions);
            assertFalse(config.unsupportedSettings.allowPistonDuplication);
        }
        assertTrue(config.unsupportedSettings.updateEquipmentOnPlayerActions);
        assertFalse(config.unsupportedSettings.allowPistonDuplication);
    }

    @Test void alreadyEnabledSwappingStaysEnabledOnDisable() throws Exception {
        Configuration config = new Configuration();
        config.unsupportedSettings.updateEquipmentOnPlayerActions = false;
        var override = PaperAttributeSwapping.enable(config);
        override.close();
        assertFalse(config.unsupportedSettings.updateEquipmentOnPlayerActions);
    }

    @Test void doesNotUndoExternalChangesWhenOriginalSettingWasAlreadyFalse() throws Exception {
        Configuration config = new Configuration();
        config.unsupportedSettings.updateEquipmentOnPlayerActions = false;
        var override = PaperAttributeSwapping.enable(config);
        config.unsupportedSettings.updateEquipmentOnPlayerActions = true;
        override.close();
        assertTrue(config.unsupportedSettings.updateEquipmentOnPlayerActions);
    }

    @Test void repeatedCleanupDoesNotOverwriteSubsequentChanges() throws Exception {
        Configuration config = new Configuration();
        var override = PaperAttributeSwapping.enable(config);
        override.close();
        config.unsupportedSettings.updateEquipmentOnPlayerActions = false;
        override.close();
        assertFalse(config.unsupportedSettings.updateEquipmentOnPlayerActions);
    }

    @Test void reloadedSettingsAreNotOverwritten() throws Exception {
        Configuration config = new Configuration();
        var override = PaperAttributeSwapping.enable(config);
        config.unsupportedSettings = new Settings();
        config.unsupportedSettings.updateEquipmentOnPlayerActions = false;
        override.close();
        assertFalse(config.unsupportedSettings.updateEquipmentOnPlayerActions);
    }

    @Test void unsupportedServerConfigurationFailsExplicitly() {
        assertThrows(NoSuchFieldException.class, () -> PaperAttributeSwapping.enable(new Object()));
        Configuration config = new Configuration();
        config.unsupportedSettings = null;
        assertThrows(NullPointerException.class, () -> PaperAttributeSwapping.enable(config));
    }
}

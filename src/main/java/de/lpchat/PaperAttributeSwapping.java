package de.lpchat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

/** Main-thread-only access to Paper's internal, non-API equipment-update setting. */
final class PaperAttributeSwapping implements AutoCloseable {
    private final Object settings;
    private final Field equipmentUpdates;
    private final boolean previousValue;
    private boolean closed;

    private PaperAttributeSwapping(Object settings, Field equipmentUpdates) throws IllegalAccessException {
        this.settings = settings;
        this.equipmentUpdates = equipmentUpdates;
        this.previousValue = equipmentUpdates.getBoolean(settings);
        equipmentUpdates.setBoolean(settings, false);
    }

    static PaperAttributeSwapping enable() throws ReflectiveOperationException {
        Class<?> configuration = Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
        return enable(configuration.getMethod("get").invoke(null));
    }

    static PaperAttributeSwapping enable(Object configuration) throws ReflectiveOperationException {
        Objects.requireNonNull(configuration, "Paper global configuration is not initialized");
        Object settings = Objects.requireNonNull(configuration.getClass().getField("unsupportedSettings")
                .get(configuration), "Paper unsupported settings are not initialized");
        Field field = settings.getClass().getField("updateEquipmentOnPlayerActions");
        if (field.getType() != boolean.class || Modifier.isStatic(field.getModifiers())
                || Modifier.isFinal(field.getModifiers())) {
            throw new NoSuchFieldException("Expected mutable boolean updateEquipmentOnPlayerActions");
        }
        return new PaperAttributeSwapping(settings, field);
    }

    @Override
    public void close() throws IllegalAccessException {
        if (closed) return;
        // Restore only the object we changed. A newly loaded Paper configuration is left alone.
        // If it was already false, we did not change the effective setting and own no override.
        if (previousValue && !equipmentUpdates.getBoolean(settings)) {
            equipmentUpdates.setBoolean(settings, true);
        }
        closed = true;
    }
}

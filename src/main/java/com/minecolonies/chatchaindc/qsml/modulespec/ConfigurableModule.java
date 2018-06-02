package com.minecolonies.chatchaindc.qsml.modulespec;

import lombok.Getter;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import uk.co.drnaylor.quickstart.config.AbstractConfigAdapter;
import uk.co.drnaylor.quickstart.config.TypedAbstractConfigAdapter;

import java.util.Optional;

/**
 * A module that has an associated section in the main configuration file.
 * <p>
 * <p>
 * The module configuration section, is usually a {@link ConfigSerializable} annotated object. The
 * {@link AbstractConfigAdapter} is the glue between the object and the config file.
 * </p>
 * <p>
 * Usually, {@link T} is of type {@link TypedAbstractConfigAdapter.StandardWithSimpleDefault}
 * </p>
 */
public abstract class ConfigurableModule<T extends AbstractConfigAdapter<?>> extends StandardModule
{
    /**
     * The config adapter.
     */
    @Getter
    private final T adapter = createConfigAdapter();

    @SuppressWarnings("unchecked")
    @Override
    public final void preEnable()
    {
        // This line shouldn't need a cast, but thanks to Java's type erasure, we need it.
        this.getChatChainDC().addToSubInjectorModule((Class<T>) adapter.getClass(), () -> adapter);
    }

    /**
     * Defines how to create the {@link AbstractConfigAdapter}
     *
     * @return The config adapter to create.
     */
    protected abstract T createConfigAdapter();

    /**
     * QSML method. Use {@link #getAdapter()} instead, if it is needed.
     *
     * @return The {@link AbstractConfigAdapter}
     */
    @Override
    public final Optional<AbstractConfigAdapter<?>> getConfigAdapter()
    {
        return Optional.of(adapter);
    }
}

package es.altia.domeadapter.shared.infrastructure.config.adapter.factory;

import es.altia.domeadapter.shared.infrastructure.config.adapter.ConfigAdapter;
import es.altia.domeadapter.shared.infrastructure.config.adapter.impl.YamlConfigAdapter;
import es.altia.domeadapter.shared.infrastructure.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigAdapterFactory {

    private final AppProperties appProperties;
    private final YamlConfigAdapter yamlConfigAdapter;

    public ConfigAdapter getAdapter() {
        return switch (appProperties.configSource()) {
            case "yaml" -> yamlConfigAdapter;
            default -> throw new IllegalArgumentException(
                    "Invalid config source: " + appProperties.configSource());
        };
    }
}

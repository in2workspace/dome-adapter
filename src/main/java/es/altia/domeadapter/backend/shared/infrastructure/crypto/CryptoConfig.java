package es.altia.domeadapter.shared.infrastructure.crypto;

import es.altia.domeadapter.shared.infrastructure.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CryptoConfig {

    private final AppConfig appConfig;

    public String getPrivateKey() {
        String privateKey = appConfig.getCryptoPrivateKey();
        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }
        return privateKey;
    }
}

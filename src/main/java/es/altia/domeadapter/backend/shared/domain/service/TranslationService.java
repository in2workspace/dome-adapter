package es.altia.domeadapter.shared.domain.service;

public interface TranslationService {
    String getLocale();
    String translate(String code, Object... args);
}

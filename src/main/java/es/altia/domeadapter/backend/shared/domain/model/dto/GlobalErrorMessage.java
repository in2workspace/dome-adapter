package es.altia.domeadapter.shared.domain.model.dto;

public record GlobalErrorMessage(
        String type,
        String title,
        int status,
        String detail,
        String instance
) {
}

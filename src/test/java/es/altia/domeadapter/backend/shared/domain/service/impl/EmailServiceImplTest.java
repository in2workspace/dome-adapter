package es.altia.domeadapter.backend.shared.domain.service.impl;

import es.altia.domeadapter.backend.shared.domain.exception.EmailCommunicationException;
import es.altia.domeadapter.backend.shared.domain.service.TranslationService;
import es.altia.domeadapter.backend.shared.infrastructure.config.AppConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceImplTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private TranslationService translationService;
    @Mock private AppConfig appConfig;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(appConfig.getMailFrom()).thenReturn("noreply@example.com");
        when(translationService.translate(anyString())).thenReturn("Email Subject");
        when(translationService.getLocale()).thenReturn("en");
        when(templateEngine.process(anyString(), any())).thenReturn("<html>body</html>");
    }

    @Test
    void sendResponseUriFailed_sendsTemplatedEmail() {
        StepVerifier.create(
                emailService.sendResponseUriFailed("to@example.com", "spec-1", "cred-1", "provider@example.com")
        ).verifyComplete();

        verify(translationService).translate("email.unsuccessful-submission");
        verify(templateEngine).process(startsWith("response-uri-failed"), any());
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendResponseUriExhausted_sendsTemplatedEmail() {
        StepVerifier.create(
                emailService.sendResponseUriExhausted("to@example.com", "spec-1", "cred-1", "provider@example.com")
        ).verifyComplete();

        verify(translationService).translate("email.retry-exhausted-submission");
        verify(templateEngine).process(startsWith("response-uri-exhausted"), any());
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendCertificationUploaded_sendsTemplatedEmail() {
        StepVerifier.create(
                emailService.sendCertificationUploaded("to@example.com", "spec-1", "cred-1")
        ).verifyComplete();

        verify(translationService).translate("email.certification-uploaded");
        verify(templateEngine).process(startsWith("certification-uploaded"), any());
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendResponseUriAcceptedWithHtml_sendsHtmlDirectly_withoutTemplate() {
        StepVerifier.create(
                emailService.sendResponseUriAcceptedWithHtml("to@example.com", "prod-1", "<html>content</html>")
        ).verifyComplete();

        verify(translationService).translate("email.missing-documents-certification");
        verify(templateEngine, never()).process(anyString(), any());
        verify(javaMailSender).send(mimeMessage);
    }

//    @Test
//    void sendResponseUriFailed_whenMessagingException_propagatesEmailCommunicationException() throws MessagingException {
//        doThrow(new MessagingException("smtp error")).when(mimeMessage).setFrom(any());
//
//        StepVerifier.create(
//                emailService.sendResponseUriFailed("to@example.com", "spec-1", "cred-1", "provider@example.com")
//        ).expectError(EmailCommunicationException.class).verify();
//    }
}

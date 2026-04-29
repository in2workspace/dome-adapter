package es.altia.domeadapter.shared.domain.service.impl;

import es.altia.domeadapter.shared.domain.exception.EmailCommunicationException;
import es.altia.domeadapter.shared.domain.service.EmailService;
import es.altia.domeadapter.shared.domain.service.TranslationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Consumer;

import static es.altia.domeadapter.shared.domain.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private final TranslationService translationService;

    @Override
    public Mono<Void> sendResponseUriFailed(String to, String productSpecificationId, String credentialId, String providerEmail, String guideUrl) {
        return sendTemplatedEmail(
                to,
                "email.unsuccessful-submission",
                "response-uri-failed",
                context -> {
                    context.setVariable(PRODUCT_SPECIFICATION_ID, productSpecificationId);
                    context.setVariable(CREDENTIAL_ID, credentialId);
                    context.setVariable("providerEmail", providerEmail);
                    context.setVariable("guideUrl", guideUrl);
                }
        );
    }

    @Override
    public Mono<Void> sendResponseUriExhausted(String to, String productSpecificationId, String credentialId, String providerEmail, String guideUrl) {
        return sendTemplatedEmail(
                to,
                "email.retry-exhausted-submission",
                "response-uri-exhausted",
                context -> {
                    context.setVariable(PRODUCT_SPECIFICATION_ID, productSpecificationId);
                    context.setVariable(CREDENTIAL_ID, credentialId);
                    context.setVariable("providerEmail", providerEmail);
                    context.setVariable("guideUrl", guideUrl);
                }
        );
    }

    @Override
    public Mono<Void> sendCertificationUploaded(String to, String productSpecificationId, String credentialId) {
        return sendTemplatedEmail(
                to,
                "email.certification-uploaded",
                "certification-uploaded",
                context -> {
                    context.setVariable(PRODUCT_SPECIFICATION_ID, productSpecificationId);
                    context.setVariable(CREDENTIAL_ID, credentialId);
                }
        );
    }

    @Override
    public Mono<Void> sendResponseUriAcceptedWithHtml(String to, String productId, String htmlContent) {
        return Mono.fromCallable(() -> {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, UTF_8);
            helper.setFrom(mailProperties.getUsername());
            helper.setTo(to);
            helper.setSubject(translationService.translate("email.missing-documents-certification") + productId);
            helper.setText(htmlContent, true);
            javaMailSender.send(mimeMessage);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private Mono<Void> sendTemplatedEmail(
            String to,
            String subjectKey,
            String templateName,
            Consumer<Context> contextCustomizer
    ) {
        return Mono.fromCallable(() -> {
            try {
                MimeMessage mimeMessage = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, UTF_8);
                helper.setFrom(mailProperties.getUsername());
                helper.setTo(to);
                helper.setSubject(translationService.translate(subjectKey));

                Context context = new Context();
                contextCustomizer.accept(context);

                String html = templateEngine.process(templateName + "-" + translationService.getLocale(), context);
                helper.setText(html, true);

                javaMailSender.send(mimeMessage);
                return null;
            } catch (MessagingException e) {
                throw new EmailCommunicationException(MAIL_ERROR_COMMUNICATION_EXCEPTION_MESSAGE);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}

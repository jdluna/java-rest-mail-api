package com.korges.demo.service;

import com.korges.demo.model.dto.input.EmailInput;
import com.korges.demo.model.dto.input.Error;
import com.korges.demo.model.entity.Email;
import com.korges.demo.model.enums.EmailStatus;
import com.korges.demo.model.enums.ErrorEnum;
import com.korges.demo.service.persistence.EmailPersistenceService;
import com.korges.demo.service.sender.EmailSenderService;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Service
public class EmailFacadeServiceImpl implements EmailFacadeService {
    private final EmailPersistenceService emailPersistenceService;
    private final EmailSenderService emailSenderService;

    private final Predicate<Email> isEmailPending =  email -> email.getEmailStatus().equals(EmailStatus.PENDING);
    private final Predicate<Email> isRecipientProvided = email -> !email.getRecipients().isEmpty();
    private final Function<Email, Email> setEmailStatusToSent = email -> new Email(email.getId(), email.getSubject(), email.getText(), email.getRecipients(), email.getAttachments(), EmailStatus.SENT, email.getPriority());

    @Override
    public List<Email> findAll() {
        return emailPersistenceService.findAll();
    }

    @Override
    public Either<Error, Email> findById(String id) {
        return emailPersistenceService.findById(id);
    }

    @Override
    public Email save(EmailInput emailDTO) {
        Email email = Email.builder()
                .subject(emailDTO.getSubject())
                .text(emailDTO.getText())
                .recipients(emailDTO.getRecipients())
                .attachments(emailDTO.getAttachments())
                .emailStatus(EmailStatus.PENDING)
                .priority(emailDTO.getPriority())
                .build();

        return emailPersistenceService.save(email);
    }

    @Override
    public Either<Error, Email> update(String id, EmailInput email) {
        return emailPersistenceService.findById(id)
                .filterOrElse(x -> x.getEmailStatus().equals(EmailStatus.PENDING), x -> Error.build(id, ErrorEnum.SENT))
                .map(x -> new Email(x.getId(), email.getSubject(), email.getText(), email.getRecipients(), email.getAttachments(), x.getEmailStatus(), email.getPriority()))
                .map(emailPersistenceService::save);
    }

    @Override
    public Either<Error, Email> send(String id) {
        return emailPersistenceService.findById(id)
                .filterOrElse(isEmailPending, x -> Error.build(id, ErrorEnum.SENT))
                .filterOrElse(isRecipientProvided, x -> Error.build(id, ErrorEnum.NO_RECIPIENTS))
                .flatMap(emailSenderService::send)
                .map(setEmailStatusToSent)
                .map(emailPersistenceService::save);
    }

    @Override
    public List<Either<Error, Email>> sendAllPending() {
        return emailPersistenceService.findAllByEmailStatus(EmailStatus.PENDING)
                .filter(isRecipientProvided)
                .map(x -> emailSenderService
                                .send(x)
                                .map(setEmailStatusToSent)
                                .map(emailPersistenceService::save)
                );
    }

    @Override
    public Either<Error, EmailStatus> findEmailStatus(String id) {
        return emailPersistenceService.findEmailStatus(id);
    }

}
package com.korges.demo.model.entity;

import com.korges.demo.model.enums.EmailStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;


@AllArgsConstructor
@Setter
@Getter
@Builder
@Document
public class Email {

    @Id
    private String id;
    private String subject;
    private String text;
    private Set<String> recipients;
    private Set<String> attachments;
    private EmailStatus emailStatus;

    public Email() {

    }

}
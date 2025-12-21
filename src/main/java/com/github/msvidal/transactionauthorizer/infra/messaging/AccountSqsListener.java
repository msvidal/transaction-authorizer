package com.github.msvidal.transactionauthorizer.infra.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.MonetaryAmount;
import com.github.msvidal.transactionauthorizer.application.usecase.CreateAccountUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;

@Component
@AllArgsConstructor
@Profile({"listener","local"})
public class AccountSqsListener {

    private static final Logger log = LoggerFactory.getLogger(AccountSqsListener.class);

    private final ObjectMapper objectMapper;
    private final CreateAccountUseCase createAccountUseCase;

    @SqsListener("${aws.sqs.queue-name}")
    public void receive(String body) {
        try {
            log.info("Mensagem recebida body={}", body);
            JsonNode root = objectMapper.readTree(body);
            JsonNode accountNode = root.has("account") ? root.get("account") : root;
            if (accountNode == null || accountNode.isMissingNode() || accountNode.isNull()) {
                log.error("Campo 'account' ausente na mensagem");
                return;
            }

            AccountDto dto = objectMapper.treeToValue(accountNode, AccountDto.class);
            createAccountUseCase.execute(toRecord(dto));

            log.info("Create account processada");

        } catch (Exception e) {
            log.error("Erro ao processar mensagem", e);
            throw new RuntimeException(e);
        }
    }

    public Account toRecord(AccountDto dto) {
        if (dto == null) return null;

        var value = dto.balance() == null ? BigDecimal.ZERO : dto.balance();
        var balance = new MonetaryAmount(value, Currency.getInstance("BRL"));
        OffsetDateTime createdAt = dto.createdAt();
        String status = dto.status() == null ? "ENABLED" : dto.status();

        return new Account(
                dto.id(),
                dto.owner(),
                balance,
                createdAt,
                status
        );
    }
}

package com.example.corebanking_service.entity;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.SavingAccountResponse;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@NamedNativeQueries({
        @NamedNativeQuery(
                name = "AccountSummaryQueryResult",
                query = """
        SELECT
            ca.account_number AS accountNumber,
            ca.cif_code AS cifCode,
            ca.account_type AS accountType,
            ca.balance AS balance,
            s.initial_deposit AS initialDeposit,
            t.term_value_months AS termValueMonths,
            t.interest_rate AS interestRate,
            t.is_active AS isActive,
            c.credit_limit AS creditLimit,
            c.current_debt AS currentDebt,
            ct.annual_fee AS annualFee,
            ct.type_name AS cardTypeName,
            ct.image_url AS cardImageUrl
        FROM core_accounts ca
        LEFT JOIN savings_account s ON ca.account_number = s.account_number
        LEFT JOIN core_term_config t ON s.term_id = t.term_id
        LEFT JOIN credit_account c ON ca.account_number = c.account_number
        LEFT JOIN core_creditcard_type ct ON c.cart_type_id = ct.cart_type_id
        WHERE ca.cif_code = :cifCode
        """,
                resultSetMapping = "AccountSummaryDTOMapping"
        ),
        @NamedNativeQuery(
                name = "SavingAccountQueryResult",
                query = """
        SELECT
            ca.status,
            ca.account_number AS accountNumber,
            ca.cif_code AS cifCode,
            ca.account_type AS accountType,
            ca.balance AS balance,
            s.initial_deposit AS initialDeposit,
            t.term_value_months AS termValueMonths,
            t.interest_rate AS interestRate,
            ca.opened_date AS openedDate
        FROM core_accounts ca
        LEFT JOIN savings_account s ON ca.account_number = s.account_number
        LEFT JOIN core_term_config t ON s.term_id = t.term_id
        LEFT JOIN credit_account c ON ca.account_number = c.account_number
        LEFT JOIN core_creditcard_type ct ON c.cart_type_id = ct.cart_type_id
        WHERE ca.cif_code = :cifCode AND ca.account_type = 'SAVING' AND ca.status='ACTIVE'
        """,
                resultSetMapping = "SavingAccountResponseMapping"
        )
})
@SqlResultSetMapping(
        name = "AccountSummaryDTOMapping",
        classes = @ConstructorResult(
                targetClass = AccountSummaryDTO.class,
                columns = {
                        @ColumnResult(name = "accountNumber", type = String.class),
                        @ColumnResult(name = "cifCode", type = String.class),
                        @ColumnResult(name = "accountType", type = String.class),
                        @ColumnResult(name = "balance", type = BigDecimal.class),
                        @ColumnResult(name = "initialDeposit", type = BigDecimal.class),
                        @ColumnResult(name = "termValueMonths", type = Integer.class),
                        @ColumnResult(name = "interestRate", type = BigDecimal.class),
                        @ColumnResult(name = "isActive", type = Boolean.class),
                        @ColumnResult(name = "creditLimit", type = BigDecimal.class),
                        @ColumnResult(name = "currentDebt", type = BigDecimal.class),
                        @ColumnResult(name = "annualFee", type = BigDecimal.class),
                        @ColumnResult(name = "cardTypeName", type = String.class),
                        @ColumnResult(name = "cardImageUrl", type = String.class)
                }
        )
)
@SqlResultSetMapping(
        name = "SavingAccountResponseMapping",
        classes = @ConstructorResult(
                targetClass = SavingAccountResponse.class,
                columns = {
                        @ColumnResult(name = "status", type = String.class),
                        @ColumnResult(name = "accountNumber", type = String.class),
                        @ColumnResult(name = "cifCode", type = String.class),
                        @ColumnResult(name = "accountType", type = String.class),
                        @ColumnResult(name = "balance", type = BigDecimal.class),
                        @ColumnResult(name = "initialDeposit", type = BigDecimal.class),
                        @ColumnResult(name = "termValueMonths", type = Integer.class),
                        @ColumnResult(name = "interestRate", type = BigDecimal.class),
                        @ColumnResult(name = "openedDate", type = LocalDate.class)
                }
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "core_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bank_code", "account_number"}))
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
public class CoreAccount {
    @Id
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType;

    @Column(name = "balance")
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AccountStatus status;

    @Column(name = "opened_date")
    private LocalDate openedDate;

    @ManyToOne
    @JoinColumn(name = "cif_code", nullable = false)
    private CoreCustomer coreCustomer;

    @OneToMany(mappedBy = "coreAccount")
    private List<CoreLoan> loans;

    @OneToMany(mappedBy = "fromAccount")
    private List<CoreTransaction> outgoingTransactions;

    @OneToMany(mappedBy = "toAccount")
    private List<CoreTransaction> incomingTransactions;
}
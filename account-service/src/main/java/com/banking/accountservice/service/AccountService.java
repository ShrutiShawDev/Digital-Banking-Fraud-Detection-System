package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import com.banking.accountservice.repository.AccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(@Valid CreateAccountRequest request) {
        log.info("Creating account for: {}", request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Account already exists for email: "+request.getEmail());
        }
        Account account = Account.builder()
                .accountHolderName(request.getAccountHolderName())
                .accountNumber(generateAccountNumber())
                .email(request.getEmail())
                .phone(request.getPhone())
                .accountType(request.getAccountType())
                .balance(request.getInitialDeposit())
                .status(AccountStatus.ACTIVE)
                .dailyTransactionLimit(
                        request.getAccountType() == AccountType.SAVINGS
                                ? new BigDecimal("100000")
                                : new BigDecimal("500000")
                )
                .build();

        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }

    public AccountResponse getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new RuntimeException("Account not found"));

        return mapToResponse(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(()->new RuntimeException("Account not found"));
        return account.getBalance();
    }

    // Block account -> called by Fraud detection service via kafka
    public void blockAccount(String  accountNumber){
        log.info("Blocking account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account blocked: {}", accountNumber);
    }

    /**
     * Deduct balance from sender account
     * Called by Transaction Service
     * @param accountNumber
     * @param amount
     */
    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("Deducting balance {} from account: {}", amount, accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));

        if(account.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account is not active "+accountNumber);
        }

        if(account.getBalance().compareTo(amount) < 0){
            throw new RuntimeException("Insufficient funds for account "+accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("Balance updated. New Balance: {}", account.getBalance());
    }

    /**
     * Called by Transaction Service via kafka
     * @param accountNumber
     * @param amount
     */
    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("Crediting {} to account: {}", amount, accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Balance Credited. New Balance: {}", account.getBalance());
    }

    // Generate unique 12-digit account number
    private String generateAccountNumber(){
        String accountNumber;

        do{
            long number = secureRandom.nextLong(1_000_000_000_000L);
            /**
             * Formats the number as a 12-digit account number.
             * Leading zeros are added when necessary.
             * %  -> format specifier
             * 0  -> pad with zeros
             * 12 -> width of 12 characters
             * d  -> decimal integer
             */
            accountNumber = String.format("%012d",number);
        }while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
    private AccountResponse mapToResponse(Account account){
        AccountResponse accountResponse = AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .balance(account.getBalance())
                .dailyTransactionLimit(account.getDailyTransactionLimit())
                .createdAt(account.getCreatedAt())
                .build();
        return accountResponse;
    }

    public String getEmail(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(()->new RuntimeException("Account not found"));
        return account.getEmail();
    }
}

package com.contractGuard.LegalLens.exception;

public class ContractNotFoundException extends RuntimeException {

    public ContractNotFoundException(String contractUuid) {
        super("Contract not found: " + contractUuid);
    }
}

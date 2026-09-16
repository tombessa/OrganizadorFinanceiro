package br.com.organizadorfinanceiro.imports;

public enum ImportExecutionStatus {
    RECEIVED,
    PARSING,
    COMPLETED,
    COMPLETED_WITH_WARNINGS,
    FAILED,
    DUPLICATE
}

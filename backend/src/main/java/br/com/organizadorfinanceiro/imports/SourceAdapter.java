package br.com.organizadorfinanceiro.imports;

public enum SourceAdapter {
    INTER_ACCOUNT_CSV(ImportTargetType.ACCOUNT, "inter-account-v1"),
    INTER_CARD_CSV(ImportTargetType.CARD, "inter-card-v1"),
    ITAU_CARD_XLSX(ImportTargetType.CARD, "itau-card-v1"),
    SANTANDER_ACCOUNT_PDF(ImportTargetType.ACCOUNT, "santander-account-v1"),
    PAYROLL_PDF(ImportTargetType.PAYROLL, "payroll-v1");

    private final ImportTargetType targetType;
    private final String version;

    SourceAdapter(ImportTargetType targetType, String version) {
        this.targetType = targetType;
        this.version = version;
    }

    public ImportTargetType targetType() { return targetType; }
    public String version() { return version; }
}

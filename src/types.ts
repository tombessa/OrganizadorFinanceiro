export type AccountType = "CHECKING" | "CASH" | "INVESTMENT";

export interface Institution {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

export interface FinancialAccount {
  id: string;
  institutionId: string;
  name: string;
  accountType: AccountType;
  currency: string;
  overdraftLimit: number;
  active: boolean;
}

export interface CreditCard {
  id: string;
  institutionId: string;
  paymentAccountId: string | null;
  name: string;
  lastFourDigits: string;
  closingDay: number;
  dueDay: number;
  active: boolean;
}

export interface ImportRegistration {
  id: string;
  adapter: SourceAdapter;
  filename: string;
  byteSize: number;
  sha256: string;
  receivedAt: string;
  storageStatus: "STORED" | "DELETED";
  executionId: string | null;
  documentStatus: "POSTED" | "PROJECTED" | null;
  executionStatus: "RECEIVED" | "PARSING" | "COMPLETED" | "COMPLETED_WITH_WARNINGS" | "FAILED" | "DUPLICATE";
  detectedRows: number;
  importedRows: number;
  duplicateRows: number;
  warningCount: number;
  duplicate: boolean;
  suggestedHttpStatus: number;
}

export type SourceAdapter =
  | "INTER_ACCOUNT_CSV"
  | "INTER_CARD_CSV"
  | "ITAU_CARD_XLSX"
  | "SANTANDER_ACCOUNT_PDF"
  | "PAYROLL_PDF";

export interface SetupCounts {
  institutions: number;
  accounts: number;
  cards: number;
}

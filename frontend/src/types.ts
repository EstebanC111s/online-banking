export type AccountType = 'SAVINGS' | 'CHECKING';

export interface Account {
  id: number;
  number: string;
  type: AccountType;
  typeLabel: string;
  status: 'OPEN' | 'CLOSED' | 'RETRACTED';
  balance: number;
  openedAt: string;
  retractDeadline?: string;
  canRetract: boolean;
}

export interface Transaction {
  id: number;
  accountId: number;
  type: 'DEPOSIT' | 'TRANSFER_OUT' | 'TRANSFER_IN' | 'FEE' | 'REFUND';
  typeLabel: string;
  amount: number;
  balanceAfter: number;
  description: string;
  reference?: string;
  counterpartyAccount?: string;
  createdAt: string;
  disputable: boolean;
}

export interface Quote {
  quoteId: string;
  fromAccountNumber: string;
  toAccountNumber: string;
  recipientName: string;
  ownAccount: boolean;
  amount: number;
  fee: number;
  total: number;
  description: string;
  status: string;
  expiresAt: string;
}

export interface TransferResult {
  reference: string;
  status: string;
  amount: number;
  fee: number;
  total: number;
  newBalance: number;
  confirmedAt: string;
}

export interface Dispute {
  id: number;
  transactionId: number;
  transactionDescription: string;
  transactionAmount: number;
  transactionType: string;
  reason: string;
  status: 'OPEN' | 'REFUNDED' | 'REJECTED';
  statusLabel: string;
  resolutionNote?: string;
  createdAt: string;
  dueDate: string;
  resolvedAt?: string;
}

export interface Me {
  id: number;
  fullName: string;
  email: string;
  birthDate?: string;
  status: string;
  role: 'CUSTOMER' | 'ADMIN';
  termsVersion?: string;
  termsAcceptedAt?: string;
  privacyVersion?: string;
  privacyAcceptedAt?: string;
  currentTermsVersion: string;
  currentPrivacyVersion: string;
  needsLegalAcceptance: boolean;
}

export interface ConsentState {
  type: 'MARKETING' | 'ANALYTICS';
  description: string;
  granted: boolean;
  updatedAt?: string;
  source?: string;
}

export interface LegalDoc {
  slug: string;
  title: string;
  version: string;
  sections: { heading: string; body: string }[];
}

export interface FeeItem {
  concept: string;
  amount: number;
  note: string;
}

export interface PromiseItem {
  claim: string;
  evidence: string;
  kept: boolean;
  howToVerify: string;
}

export interface ReviewView {
  id: number;
  authorName: string;
  verifiedCustomer: boolean;
  rating: number;
  comment: string;
  createdAt: string;
  editedAt?: string;
}

export interface ReviewSummary {
  count: number;
  average: number;
  distribution: Record<string, number>;
  reviews: ReviewView[];
}

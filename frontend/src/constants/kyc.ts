import type { AnnualIncome, IdType, SourceOfFunds } from '../types';

export interface Option<T extends string = string> {
  value: T;
  label: string;
}

/**
 * Representative list of countries (ISO 3166-1 alpha-2), sorted by label.
 * Intentionally includes CU, IR, KP, SD and SY - the downstream risk-assessment
 * workflow flags these as high-risk jurisdictions for the demo.
 */
export const COUNTRIES: Option[] = [
  { value: 'AR', label: 'Argentina' },
  { value: 'AU', label: 'Australia' },
  { value: 'AT', label: 'Austria' },
  { value: 'BE', label: 'Belgium' },
  { value: 'BR', label: 'Brazil' },
  { value: 'CA', label: 'Canada' },
  { value: 'CN', label: 'China' },
  { value: 'CU', label: 'Cuba' },
  { value: 'DK', label: 'Denmark' },
  { value: 'EG', label: 'Egypt' },
  { value: 'FR', label: 'France' },
  { value: 'DE', label: 'Germany' },
  { value: 'HK', label: 'Hong Kong' },
  { value: 'IN', label: 'India' },
  { value: 'ID', label: 'Indonesia' },
  { value: 'IR', label: 'Iran' },
  { value: 'IE', label: 'Ireland' },
  { value: 'IL', label: 'Israel' },
  { value: 'IT', label: 'Italy' },
  { value: 'JP', label: 'Japan' },
  { value: 'KE', label: 'Kenya' },
  { value: 'MX', label: 'Mexico' },
  { value: 'NL', label: 'Netherlands' },
  { value: 'NZ', label: 'New Zealand' },
  { value: 'NG', label: 'Nigeria' },
  { value: 'KP', label: 'North Korea' },
  { value: 'NO', label: 'Norway' },
  { value: 'PK', label: 'Pakistan' },
  { value: 'PH', label: 'Philippines' },
  { value: 'PL', label: 'Poland' },
  { value: 'PT', label: 'Portugal' },
  { value: 'SA', label: 'Saudi Arabia' },
  { value: 'SG', label: 'Singapore' },
  { value: 'ZA', label: 'South Africa' },
  { value: 'KR', label: 'South Korea' },
  { value: 'ES', label: 'Spain' },
  { value: 'SD', label: 'Sudan' },
  { value: 'SE', label: 'Sweden' },
  { value: 'CH', label: 'Switzerland' },
  { value: 'SY', label: 'Syria' },
  { value: 'TR', label: 'Turkey' },
  { value: 'AE', label: 'United Arab Emirates' },
  { value: 'GB', label: 'United Kingdom' },
  { value: 'US', label: 'United States' },
  { value: 'VN', label: 'Vietnam' },
];

const COUNTRY_LABELS: Record<string, string> = Object.fromEntries(
  COUNTRIES.map((c) => [c.value, c.label]),
);

export function countryLabel(code: string | null | undefined): string {
  if (!code) return '—';
  return COUNTRY_LABELS[code] ?? code;
}

export const GENDERS: Option[] = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
  { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
];

export const ID_TYPES: Option<IdType>[] = [
  { value: 'PASSPORT', label: 'Passport' },
  { value: 'NATIONAL_ID', label: 'National ID' },
  { value: 'DRIVERS_LICENSE', label: "Driver's License" },
];

export const ANNUAL_INCOMES: Option<AnnualIncome>[] = [
  { value: 'UNDER_25K', label: 'Under $25k' },
  { value: '25K_TO_50K', label: '$25k – $50k' },
  { value: '50K_TO_100K', label: '$50k – $100k' },
  { value: '100K_TO_250K', label: '$100k – $250k' },
  { value: 'OVER_250K', label: 'Over $250k' },
];

const INCOME_LABELS: Record<string, string> = Object.fromEntries(
  ANNUAL_INCOMES.map((o) => [o.value, o.label]),
);

export function annualIncomeLabel(value: string | null | undefined): string {
  if (!value) return '—';
  return INCOME_LABELS[value] ?? value;
}

export const SOURCES_OF_FUNDS: Option<SourceOfFunds>[] = [
  { value: 'EMPLOYMENT', label: 'Employment' },
  { value: 'BUSINESS', label: 'Business Income' },
  { value: 'INVESTMENTS', label: 'Investments' },
  { value: 'INHERITANCE', label: 'Inheritance' },
  { value: 'OTHER', label: 'Other' },
];

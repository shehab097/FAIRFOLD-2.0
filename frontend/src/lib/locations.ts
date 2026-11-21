import { Country } from 'country-state-city';

export interface CountryOption {
  name: string;
  isoCode: string;
}

/** All countries, sorted alphabetically by display name. */
export const COUNTRIES: CountryOption[] = Country.getAllCountries()
  .map((c) => ({ name: c.name, isoCode: c.isoCode }))
  .sort((a, b) => a.name.localeCompare(b.name));

const countryByName = new Map(COUNTRIES.map((c) => [c.name.toLowerCase(), c]));
const cityCache = new Map<string, string[]>();

let cityModule: Promise<typeof import('country-state-city/lib/city')> | null = null;

/**
 * City names for a country (matched by name), sorted alphabetically. The city
 * dataset is ~8 MB, so it is loaded lazily as its own chunk on first use.
 */
export async function citiesForCountry(countryName: string): Promise<string[]> {
  const country = countryByName.get(countryName.trim().toLowerCase());
  if (!country) return [];
  const cached = cityCache.get(country.isoCode);
  if (cached) return cached;
  cityModule ??= import('country-state-city/lib/city');
  const { default: City } = await cityModule;
  const names = (City.getCitiesOfCountry(country.isoCode) ?? [])
    .map((c) => c.name)
    .sort((a, b) => a.localeCompare(b));
  const unique = Array.from(new Set(names));
  cityCache.set(country.isoCode, unique);
  return unique;
}

export function isKnownCountry(name: string): boolean {
  return countryByName.has(name.trim().toLowerCase());
}

export interface LocationValue {
  country: string;
  city: string;
  address: string;
}

export function emptyLocation(): LocationValue {
  return { country: '', city: '', address: '' };
}

/** "Street, City, Country" (skips empty parts). */
export function formatLocation(value: LocationValue): string {
  return [value.address, value.city, value.country]
    .map((part) => part.trim())
    .filter(Boolean)
    .join(', ');
}

/**
 * Best-effort reverse of {@link formatLocation}. If the last part is a known
 * country it becomes the country, the previous part the city, and everything
 * before it the address. Otherwise the whole string is kept as the address.
 */
export function parseLocation(value: string | null | undefined): LocationValue {
  const text = (value ?? '').trim();
  if (!text) return emptyLocation();
  const parts = text.split(',').map((p) => p.trim()).filter(Boolean);
  const last = parts[parts.length - 1] ?? '';
  if (!isKnownCountry(last)) {
    return { country: '', city: '', address: text };
  }
  const rest = parts.slice(0, -1);
  const city = rest.length > 0 ? rest[rest.length - 1] : '';
  const address = rest.slice(0, -1).join(', ');
  return { country: countryByName.get(last.toLowerCase())?.name ?? '', city, address };
}

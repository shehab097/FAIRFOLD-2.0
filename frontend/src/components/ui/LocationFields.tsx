import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Input } from './Input';
import { Select } from './Select';
import { COUNTRIES, citiesForCountry, isKnownCountry, type LocationValue } from '../../lib/locations';

interface LocationFieldsProps {
  value: LocationValue;
  onChange: (value: LocationValue) => void;
  requiredCountry?: boolean;
  countryError?: string;
  /** Hide the street-address field when only city/country are needed. */
  showAddress?: boolean;
}

const labelClass = 'mb-1 block text-xs text-text-secondary';

interface PhotonFeature {
  properties?: {
    name?: string;
    street?: string;
    housenumber?: string;
    city?: string;
    state?: string;
    country?: string;
  };
}

function photonLabel(feature: PhotonFeature): string {
  const p = feature.properties ?? {};
  const street = [p.street, p.housenumber].filter(Boolean).join(' ');
  const parts = [street || p.name, p.city, p.state, p.country].filter(Boolean) as string[];
  return Array.from(new Set(parts)).join(', ');
}

function useAddressSuggestions(query: string, city: string, country: string): string[] {
  const [suggestions, setSuggestions] = useState<string[]>([]);

  useEffect(() => {
    const text = [query.trim(), city.trim(), country.trim()].filter(Boolean).join(', ');
    if (query.trim().length < 4) {
      setSuggestions([]);
      return;
    }
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      const params = new URLSearchParams({ q: text, limit: '6', lang: 'en' });
      fetch(`https://photon.komoot.io/api/?${params.toString()}`, {
        signal: controller.signal,
        headers: { Accept: 'application/json' },
      })
        .then((res) => (res.ok ? res.json() : { features: [] }))
        .then((body: { features?: PhotonFeature[] }) => {
          const labels = (body.features ?? []).map(photonLabel).filter(Boolean);
          setSuggestions(Array.from(new Set(labels)));
        })
        .catch(() => undefined);
    }, 700);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [query, city, country]);

  return suggestions;
}

function CityCombobox({
  value,
  onChange,
  cities,
  disabled,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  cities: string[];
  disabled?: boolean;
  placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const closeTimer = useRef<number | undefined>(undefined);

  const matches = useMemo(() => {
    const query = value.trim().toLowerCase();
    if (!query) return cities.slice(0, 50);
    const starts: string[] = [];
    const contains: string[] = [];
    for (const city of cities) {
      const lower = city.toLowerCase();
      if (lower.startsWith(query)) starts.push(city);
      else if (lower.includes(query)) contains.push(city);
      if (starts.length >= 50) break;
    }
    return [...starts, ...contains].slice(0, 50);
  }, [cities, value]);

  return (
    <div className="relative">
      <Input
        value={value}
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
        }}
        onFocus={() => {
          window.clearTimeout(closeTimer.current);
          setOpen(true);
        }}
        onBlur={() => {
          closeTimer.current = window.setTimeout(() => setOpen(false), 150);
        }}
        placeholder={placeholder}
        disabled={disabled}
        autoComplete="off"
        role="combobox"
        aria-expanded={open}
      />
      {open && !disabled && matches.length > 0 && (
        <div
          role="listbox"
          className="absolute z-20 mt-1 max-h-60 w-full overflow-auto rounded-lg border border-border bg-raised py-1 shadow-lg"
        >
          {matches.map((city) => (
            <button
              key={city}
              type="button"
              role="option"
              aria-selected={city === value}
              className="block w-full px-3 py-2 text-left text-sm text-text hover:bg-primary-subtle"
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => {
                onChange(city);
                setOpen(false);
              }}
            >
              {city}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

/**
 * Cascading country + city dropdowns plus a free OpenStreetMap address
 * autocomplete. Country and city use the bundled `country-state-city` data,
 * so they work offline; address suggestions come from the free Photon API
 * (no key required, designed for autocomplete).
 */
export function LocationFields({
  value,
  onChange,
  requiredCountry = false,
  countryError,
  showAddress = true,
}: LocationFieldsProps) {
  const addressListId = useId();
  const [cities, setCities] = useState<string[]>([]);
  const suggestions = useAddressSuggestions(value.address, value.city, value.country);

  useEffect(() => {
    let active = true;
    setCities([]);
    if (!value.country) return undefined;
    citiesForCountry(value.country).then((names) => {
      if (active) setCities(names);
    });
    return () => {
      active = false;
    };
  }, [value.country]);

  const countryNames = useMemo(() => {
    const names = COUNTRIES.map((c) => c.name);
    return value.country && !isKnownCountry(value.country) ? [value.country, ...names] : names;
  }, [value.country]);

  function update(patch: Partial<LocationValue>) {
    onChange({ ...value, ...patch });
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <div>
        <label className={labelClass}>Country</label>
        <Select
          value={value.country}
          onChange={(e) => update({ country: e.target.value, city: '' })}
          required={requiredCountry}
          error={Boolean(countryError)}
        >
          <option value="">Select country…</option>
          {countryNames.map((name) => (
            <option key={name} value={name}>
              {name}
            </option>
          ))}
        </Select>
        {countryError && <p className="mt-1 text-sm text-accent">{countryError}</p>}
      </div>

      <div>
        <label className={labelClass}>City</label>
        <CityCombobox
          value={value.city}
          onChange={(city) => update({ city })}
          cities={cities}
          disabled={!value.country}
          placeholder={value.country ? 'Search or select a city' : 'Select a country first'}
        />
      </div>

      {showAddress && (
        <div className="sm:col-span-2">
          <label className={labelClass}>Address</label>
          <Input
            value={value.address}
            onChange={(e) => update({ address: e.target.value })}
            list={addressListId}
            placeholder="Start typing an address to see suggestions"
            autoComplete="off"
          />
          <datalist id={addressListId}>
            {suggestions.map((s) => (
              <option key={s} value={s} />
            ))}
          </datalist>
        </div>
      )}
    </div>
  );
}

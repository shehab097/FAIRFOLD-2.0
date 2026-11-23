import { useEffect, useState } from 'react';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import {
  useCertifications,
  useCreateCertification,
  useDeleteCertification,
  useUpdateCertification,
} from '../queries';
import type { Certification, CertificationInput } from '../types';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDay } from '../../../lib/format';

const EMPTY: CertificationInput = {
  name: '',
  issuer: '',
  issuedOn: null,
  expiresOn: null,
  credentialId: null,
};

function toInput(certification: Certification): CertificationInput {
  return {
    name: certification.name,
    issuer: certification.issuer,
    issuedOn: certification.issuedOn,
    expiresOn: certification.expiresOn,
    credentialId: certification.credentialId,
  };
}

export function CertificationsSection() {
  const { data: certifications, isLoading, isError, refetch } = useCertifications();
  const create = useCreateCertification();
  const update = useUpdateCertification();
  const remove = useDeleteCertification();
  const { toast } = useToast();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Certification | null>(null);
  const [form, setForm] = useState<CertificationInput>(EMPTY);
  const [toDelete, setToDelete] = useState<Certification | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!formOpen) return;
    setForm(editing ? toInput(editing) : EMPTY);
    setError(null);
  }, [formOpen, editing]);

  function field(key: keyof CertificationInput, value: string) {
    setForm((current) => ({
      ...current,
      [key]: value === '' ? (key === 'name' || key === 'issuer' ? '' : null) : value,
    }));
  }

  async function save() {
    setError(null);
    if (!form.name.trim() || !form.issuer.trim()) {
      setError('Name and issuer are required.');
      return;
    }
    try {
      if (editing) {
        await update.mutateAsync({ id: editing.id, input: form });
        toast('Certification updated.', 'success');
      } else {
        await create.mutateAsync(form);
        toast('Certification added.', 'success');
      }
      setFormOpen(false);
      setEditing(null);
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await remove.mutateAsync(toDelete.id);
      toast('Certification deleted.', 'success');
      setToDelete(null);
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Certifications</h2>
          <p className="text-sm text-text-secondary">
            Add credentials from courses, vendors or professional bodies.
          </p>
        </div>
        <Button
          onClick={() => {
            setEditing(null);
            setFormOpen(true);
          }}
        >
          <Icon icon={Plus} size={16} />
          Add certification
        </Button>
      </div>

      {isLoading && <Skeleton className="h-32 w-full rounded-xl" />}
      {isError && (
        <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
          Could not load certifications.{' '}
          <button type="button" className="underline" onClick={() => refetch()}>
            Retry
          </button>
        </div>
      )}

      {!isLoading && !isError && (certifications?.length ?? 0) === 0 && (
        <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
          No certifications yet.
        </div>
      )}

      {!isLoading && !isError && (certifications?.length ?? 0) > 0 && (
        <ul className="divide-y divide-border rounded-xl border border-border bg-surface shadow-sm">
          {certifications?.map((certification) => (
            <li
              key={certification.id}
              className="flex flex-wrap items-start justify-between gap-3 p-4"
            >
              <div>
                <div className="font-semibold text-text">{certification.name}</div>
                <div className="text-sm text-text-secondary">{certification.issuer}</div>
                <div className="mt-1 text-xs text-text-subtle">
                  {certification.issuedOn ? `Issued ${formatDay(certification.issuedOn)}` : 'No issue date'}
                  {certification.expiresOn
                    ? ` · Expires ${formatDay(certification.expiresOn)}`
                    : ''}
                  {certification.credentialId ? ` · ID ${certification.credentialId}` : ''}
                </div>
              </div>
              <div className="flex gap-1">
                <Button
                  variant="ghost"
                  className="h-8 px-2 text-xs"
                  onClick={() => {
                    setEditing(certification);
                    setFormOpen(true);
                  }}
                >
                  <Icon icon={Pencil} size={14} /> Edit
                </Button>
                <Button
                  variant="danger"
                  className="h-8 px-2 text-xs"
                  onClick={() => setToDelete(certification)}
                >
                  <Icon icon={Trash2} size={14} /> Delete
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Dialog
        open={formOpen}
        title={editing ? 'Edit certification' : 'Add certification'}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
      >
        <div className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-xs text-text-secondary">Name</span>
            <Input value={form.name} onChange={(e) => field('name', e.target.value)} maxLength={255} />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs text-text-secondary">Issuer</span>
            <Input
              value={form.issuer}
              onChange={(e) => field('issuer', e.target.value)}
              maxLength={255}
            />
          </label>
          <div className="grid grid-cols-2 gap-3">
            <label className="block">
              <span className="mb-1 block text-xs text-text-secondary">Issued on</span>
              <Input
                type="date"
                value={form.issuedOn ?? ''}
                onChange={(e) => field('issuedOn', e.target.value)}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-text-secondary">Expires on</span>
              <Input
                type="date"
                value={form.expiresOn ?? ''}
                onChange={(e) => field('expiresOn', e.target.value)}
              />
            </label>
          </div>
          <label className="block">
            <span className="mb-1 block text-xs text-text-secondary">Credential ID (optional)</span>
            <Input
              value={form.credentialId ?? ''}
              onChange={(e) => field('credentialId', e.target.value)}
              maxLength={255}
            />
          </label>
          {error && <p className="text-sm text-accent">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button
              variant="secondary"
              onClick={() => {
                setFormOpen(false);
                setEditing(null);
              }}
            >
              Cancel
            </Button>
            <Button loading={create.isPending || update.isPending} onClick={save}>
              Save
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog open={Boolean(toDelete)} title="Delete certification" onClose={() => setToDelete(null)}>
        <p className="text-sm text-text">Delete “{toDelete?.name}”?</p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setToDelete(null)}>
            Cancel
          </Button>
          <Button variant="danger" loading={remove.isPending} onClick={confirmDelete}>
            Delete
          </Button>
        </div>
      </Dialog>
    </div>
  );
}

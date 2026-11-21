import { useRef, useState, type ChangeEvent } from 'react';
import { Camera, Trash2 } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { deleteProfilePhoto, uploadProfilePhoto } from '../../../api/account';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { UserAvatar } from '../../../components/ui/UserAvatar';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

export function ProfilePhotoCard() {
  const { user, updateUser } = useAuth();
  const { toast } = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);

  if (!user) return null;
  const canEdit = user.role === 'CANDIDATE' || user.role === 'EMPLOYER_HR';
  if (!canEdit) return null;

  const name = `${user.firstName} ${user.lastName}`.trim();

  async function onFile(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (file.size > MAX_PHOTO_BYTES) {
      toast('Profile photo must be at most 5 MB.', 'error');
      return;
    }

    setBusy(true);
    try {
      updateUser(await uploadProfilePhoto(file));
      toast('Profile photo updated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  }

  async function onRemove() {
    setBusy(true);
    try {
      updateUser(await deleteProfilePhoto());
      toast('Profile photo removed.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-col gap-4 rounded-xl border border-border bg-surface p-6 shadow-sm sm:flex-row sm:items-center sm:justify-between">
      <div className="flex items-center gap-4">
        <UserAvatar name={name} avatarUrl={user.avatarUrl} size="xl" />
        <div>
          <h2 className="text-base font-semibold">Profile photo</h2>
          <p className="mt-0.5 text-xs text-text-secondary">
            PNG, JPEG, GIF or WebP · converted to 480×480 WebP · max 5 MB.
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <input
          ref={inputRef}
          type="file"
          accept="image/png,image/jpeg,image/gif,image/webp"
          className="sr-only"
          onChange={onFile}
        />
        <Button variant="secondary" loading={busy} onClick={() => inputRef.current?.click()}>
          <Icon icon={Camera} size={16} />
          {user.avatarUrl ? 'Change photo' : 'Upload photo'}
        </Button>
        {user.avatarUrl && (
          <Button variant="secondary" onClick={onRemove} disabled={busy}>
            <Icon icon={Trash2} size={16} />
            Remove
          </Button>
        )}
      </div>
    </div>
  );
}

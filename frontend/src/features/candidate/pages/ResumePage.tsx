import { useRef, useState } from 'react';
import { useResume, useUploadResume } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorCode, getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function ResumePage() {
  const { data: resume, isLoading, isError, error } = useResume();
  const upload = useUploadResume();
  const { toast } = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const noResume = isError && getApiErrorCode(error) === 'RESUME_NOT_FOUND';

  async function handleFile(file: File | undefined) {
    if (!file) return;
    if (file.size > 10 * 1024 * 1024) {
      toast('Resume must be at most 10 MB.', 'error');
      return;
    }
    try {
      await upload.mutateAsync(file);
      toast('Resume uploaded.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Resume</h1>
        <p className="text-text-secondary">Upload your PDF resume (English resumes only).</p>
      </div>

      {isLoading && <Skeleton className="h-40 w-full" />}

      {isError && !noResume && <Banner tone="error">Could not load your resume.</Banner>}

      {!isLoading && (
        <Card>
          <button
            type="button"
            onDragOver={(e) => {
              e.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragging(false);
              handleFile(e.dataTransfer.files[0]);
            }}
            onClick={() => inputRef.current?.click()}
            className={
              'flex w-full flex-col items-center justify-center gap-3 border border-dashed px-6 py-10 text-center ' +
              (dragging ? 'border-primary bg-raised' : 'border-border-strong bg-bg')
            }
          >
            <span className="text-sm text-text">Drag and drop a PDF here, or click to browse.</span>
            <span className="text-xs text-text-subtle">PDF only · max 10 MB · English resumes</span>
          </button>
          <input
            ref={inputRef}
            type="file"
            accept="application/pdf"
            className="hidden"
            onChange={(e) => {
              handleFile(e.target.files?.[0]);
              e.currentTarget.value = '';
            }}
          />
        </Card>
      )}

      {noResume && (
        <Banner tone="info">No resume uploaded yet. Upload one to apply to jobs.</Banner>
      )}

      {resume && (
        <Card>
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center gap-3">
                <span className="font-medium text-text">{resume.filename ?? 'Resume'}</span>
                <Badge status={resume.status} />
              </div>
              <p className="mt-1 text-sm text-text-secondary">
                {formatSize(resume.sizeBytes)} · uploaded {formatDate(resume.uploadedAt)}
              </p>
            </div>
            <Button variant="secondary" onClick={() => inputRef.current?.click()}>
              Replace
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}

import { useEffect, useRef, useState } from 'react';
import { ExternalLink, Eye, EyeOff, FileText, Upload } from 'lucide-react';
import { getResumeFileBlob } from '../../../api/candidate';
import { useResume, useUploadResume } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
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
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);

  const noResume = isError && getApiErrorCode(error) === 'RESUME_NOT_FOUND';

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  async function ensurePreviewUrl(): Promise<string | null> {
    if (previewUrl) return previewUrl;
    setLoadingPreview(true);
    try {
      const blob = await getResumeFileBlob();
      const url = URL.createObjectURL(blob);
      setPreviewUrl(url);
      return url;
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
      return null;
    } finally {
      setLoadingPreview(false);
    }
  }

  async function togglePreview() {
    if (previewOpen) {
      setPreviewOpen(false);
      return;
    }
    const url = await ensurePreviewUrl();
    if (url) setPreviewOpen(true);
  }

  async function openInNewTab() {
    const url = await ensurePreviewUrl();
    if (url) window.open(url, '_blank', 'noopener,noreferrer');
  }

  async function handleFile(file: File | undefined) {
    if (!file) return;
    if (file.size > 10 * 1024 * 1024) {
      toast('Resume must be at most 10 MB.', 'error');
      return;
    }
    try {
      await upload.mutateAsync(file);
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      const localUrl = URL.createObjectURL(file);
      setPreviewUrl(localUrl);
      setPreviewOpen(true);
      toast('Resume uploaded.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Resume</h1>
        <p className="text-sm text-text-secondary">Upload and preview your PDF resume (English resumes only).</p>
      </div>

      {isLoading && <Skeleton className="h-40 w-full" />}

      {isError && !noResume && <Banner tone="error">Could not load your resume.</Banner>}

      {!isLoading && (
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
            'flex w-full flex-col items-center justify-center gap-2 rounded border border-dashed px-6 py-10 text-center ' +
            (dragging ? 'border-primary-text bg-primary-subtle' : 'border-border bg-surface hover:bg-raised')
          }
        >
          <Icon icon={Upload} size={22} className="text-primary-text" />
          <span className="text-sm font-medium text-text">Drag and drop a PDF here, or click to browse</span>
          <span className="text-xs text-text-subtle">PDF only · max 10 MB · English resumes</span>
        </button>
      )}
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

      {noResume && <Banner tone="info">No resume uploaded yet. Upload one to apply to jobs.</Banner>}

      {resume && (
        <div className="divide-y divide-border rounded border border-border bg-surface">
          <div className="flex flex-col justify-between gap-4 p-5 sm:flex-row sm:items-center">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-sm bg-primary-subtle text-primary-text">
                <Icon icon={FileText} size={20} />
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2.5">
                  <span className="font-semibold text-text">{resume.filename ?? 'Resume.pdf'}</span>
                  <Badge status={resume.status} />
                </div>
                <p className="mt-0.5 text-xs text-text-secondary">
                  {formatSize(resume.sizeBytes)} · uploaded {formatDate(resume.uploadedAt)}
                </p>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <Button variant="secondary" loading={loadingPreview} onClick={togglePreview}>
                <Icon icon={previewOpen ? EyeOff : Eye} size={16} />
                {previewOpen ? 'Hide preview' : 'Preview'}
              </Button>
              <Button variant="secondary" onClick={openInNewTab}>
                <Icon icon={ExternalLink} size={16} />
                New tab
              </Button>
              <Button variant="secondary" onClick={() => inputRef.current?.click()}>
                Replace
              </Button>
            </div>
          </div>

          {previewOpen && previewUrl && (
            <div className="p-4">
              <iframe
                src={previewUrl}
                title={resume.filename ?? 'Resume preview'}
                className="h-[720px] w-full rounded-sm border border-border bg-bg"
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}

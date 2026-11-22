import { useEffect, useRef, useState } from 'react';
import { AlertTriangle, ExternalLink, Eye, EyeOff, FileText, Upload } from 'lucide-react';
import { getResumeFileBlob } from '../../../api/candidate';
import {
  useApplyResumeSuggestions,
  useReportWrongStrip,
  useResume,
  useResumeSuggestions,
  useUploadResume,
} from '../queries';
import { StrippedPreview } from '../components/StrippedPreview';
import { SuggestionEditor } from '../components/SuggestionEditor';
import { Banner } from '../../../components/ui/Banner';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Tabs } from '../../../components/ui/Tabs';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorCode, getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

const PROCESSING_STEPS = ['Uploaded', 'Reading text', 'Removing personal details', 'Extracting profile'];

export function ResumePage() {
  const { data: resume, isLoading, isError, error } = useResume();
  const upload = useUploadResume();
  const ready = resume?.status === 'ready';
  const suggestions = useResumeSuggestions(Boolean(ready));
  const applySuggestions = useApplyResumeSuggestions();
  const reportStrip = useReportWrongStrip();
  const { toast } = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [tab, setTab] = useState<'preview' | 'details'>('preview');
  const [step, setStep] = useState(1);
  const [reportOpen, setReportOpen] = useState(false);
  const [reportNote, setReportNote] = useState('');
  const [reportExcerpt, setReportExcerpt] = useState('');

  const noResume = isError && getApiErrorCode(error) === 'RESUME_NOT_FOUND';
  const failed = resume?.status === 'failed';
  const processing = resume?.status === 'processing';

  useEffect(() => {
    return () => {
      if (previewUrl) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  useEffect(() => {
    if (!processing) {
      setStep(1);
      return;
    }
    const timer = window.setInterval(() => {
      setStep((current) => (current < PROCESSING_STEPS.length - 1 ? current + 1 : current));
    }, 1500);
    return () => window.clearInterval(timer);
  }, [processing]);

  useEffect(() => {
    if (ready) setTab('preview');
  }, [ready]);

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
      setPreviewUrl(null);
      setPreviewOpen(false);
      setTab('preview');
      toast('Resume uploaded. Reading it now…', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function submitReport() {
    if (!reportNote.trim()) {
      toast('Describe what was stripped wrongly.', 'error');
      return;
    }
    try {
      await reportStrip.mutateAsync({
        note: reportNote.trim(),
        excerpt: reportExcerpt.trim() || undefined,
      });
      toast('Thanks — your report was sent.', 'success');
      setReportOpen(false);
      setReportNote('');
      setReportExcerpt('');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const dropzone = (
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
        'flex w-full flex-col items-center justify-center gap-2 rounded-xl border border-dashed px-6 py-10 text-center shadow-sm ' +
        (dragging ? 'border-primary-text bg-primary-subtle' : 'border-border bg-surface hover:bg-raised')
      }
    >
      <Icon icon={Upload} size={22} className="text-primary-text" />
      <span className="text-sm font-medium text-text">Drag and drop a PDF here, or click to browse</span>
      <span className="text-xs text-text-subtle">PDF only · max 10 MB · English resumes</span>
    </button>
  );

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Resume</h1>
        <p className="text-sm text-text-secondary">
          FairFold reads your PDF, removes personal details, and suggests profile details you can review.
        </p>
      </div>

      {isLoading && <Skeleton className="h-40 w-full" />}
      {isError && !noResume && <Banner tone="error">Could not load your resume.</Banner>}
      {noResume && <Banner tone="info">No resume uploaded yet. Upload one to apply to jobs.</Banner>}

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

      {(noResume || failed) && !isLoading && dropzone}

      {resume && (
        <div className="divide-y divide-border rounded-xl border border-border bg-surface shadow-sm">
          <div className="flex flex-col justify-between gap-4 p-5 sm:flex-row sm:items-center">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
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
                {previewOpen ? 'Hide PDF' : 'PDF'}
              </Button>
              <Button variant="secondary" onClick={openInNewTab}>
                <Icon icon={ExternalLink} size={16} />
                New tab
              </Button>
              <Button variant="secondary" disabled={processing} onClick={() => inputRef.current?.click()}>
                Replace
              </Button>
            </div>
          </div>

          {previewOpen && previewUrl && (
            <div className="p-4">
              <iframe
                src={previewUrl}
                title={resume.filename ?? 'Resume preview'}
                className="h-[720px] w-full rounded-xl border border-border bg-bg"
              />
            </div>
          )}

          {processing && (
            <div className="space-y-4 p-5">
              <p className="text-sm font-medium text-text">Processing your resume…</p>
              <ol className="grid gap-3 sm:grid-cols-4">
                {PROCESSING_STEPS.map((label, index) => {
                  const done = index < step;
                  const active = index === step;
                  return (
                    <li
                      key={label}
                      className={
                        'rounded-lg border px-3 py-2 text-xs font-medium ' +
                        (done
                          ? 'border-border bg-primary-subtle text-primary-text'
                          : active
                            ? 'border-primary-text bg-surface text-text'
                            : 'border-border bg-surface text-text-subtle')
                      }
                    >
                      {index + 1}. {label}
                    </li>
                  );
                })}
              </ol>
              <p className="text-xs text-text-subtle">This page refreshes automatically every 2 seconds.</p>
            </div>
          )}

          {failed && (
            <div className="space-y-3 p-5">
              <Banner tone="error">
                {resume.failureMessage ?? 'We could not process this resume.'}
              </Banner>
              {resume.failureReason && (
                <p className="text-xs text-text-subtle">Reason code: {resume.failureReason}</p>
              )}
            </div>
          )}
        </div>
      )}

      {ready && resume && (
        <div className="space-y-4">
          <Tabs
            tabs={[
              { key: 'preview', label: 'Stripped preview' },
              { key: 'details', label: 'Extracted details' },
            ]}
            active={tab}
            onChange={(key) => setTab(key as 'preview' | 'details')}
          />

          {tab === 'preview' && (
            <div className="space-y-4">
              <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
                <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 className="text-base font-semibold text-text">What employers and AI will see</h2>
                    <p className="text-xs text-text-secondary">
                      Bracketed tokens mark personal details that were removed. Hover a token to see the original value on your own preview.
                    </p>
                  </div>
                  <Button variant="secondary" onClick={() => setReportOpen(true)}>
                    <Icon icon={AlertTriangle} size={16} />
                    Report a wrong strip
                  </Button>
                </div>
                {resume.strippedText ? (
                  <StrippedPreview
                    text={resume.strippedText}
                    placeholders={resume.droppedPlaceholders ?? []}
                  />
                ) : (
                  <p className="text-sm text-text-subtle">No stripped text available.</p>
                )}
              </div>
            </div>
          )}

          {tab === 'details' && (
            <div className="space-y-4">
              {suggestions.data?.provider === 'rule_based' && (
                <Banner tone="warning">
                  AI was unavailable, so these suggestions come from simple rules. Fill in anything
                  missing by hand before saving.
                </Banner>
              )}
              {suggestions.isLoading && <Skeleton className="h-64 w-full" />}
              {suggestions.isError && (
                <Banner tone="error">Could not load the extracted details. Try refreshing.</Banner>
              )}
              {suggestions.data && (
                <SuggestionEditor
                  suggestions={suggestions.data}
                  saving={applySuggestions.isPending}
                  onSave={async (input) => {
                    await applySuggestions.mutateAsync(input);
                  }}
                />
              )}
            </div>
          )}
        </div>
      )}

      <Dialog open={reportOpen} title="Report a wrong strip" onClose={() => setReportOpen(false)}>
        <div className="space-y-4">
          <p className="text-sm text-text-secondary">
            Tell us what was removed by mistake or what was missed. This is shared with FairFold
            admins only.
          </p>
          <div>
            <label className="mb-1 block text-xs text-text-secondary">What went wrong?</label>
            <textarea
              value={reportNote}
              onChange={(e) => setReportNote(e.target.value)}
              rows={3}
              className="w-full rounded-md border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
              placeholder="My project name was replaced with an employer chip."
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Excerpt (optional)</label>
            <input
              value={reportExcerpt}
              onChange={(e) => setReportExcerpt(e.target.value)}
              className="w-full rounded-md border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
              placeholder="Copy the text around the mistake"
            />
          </div>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setReportOpen(false)}>
              Cancel
            </Button>
            <Button loading={reportStrip.isPending} onClick={submitReport}>
              Send report
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}

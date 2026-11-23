import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, Scale, ShieldCheck, type LucideIcon } from 'lucide-react';
import { Icon } from '../components/ui/Icon';

const LAST_UPDATED = '23 November 2026';

function LegalLayout({
  title,
  subtitle,
  icon,
  contact,
  children,
}: {
  title: string;
  subtitle: string;
  icon: LucideIcon;
  contact: string;
  children: ReactNode;
}) {
  return (
    <article className="mx-auto max-w-3xl py-2 sm:py-4">
      <Link
        to="/"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-text-secondary transition-colors hover:text-text"
      >
        <Icon icon={ArrowLeft} size={15} />
        Back to home
      </Link>

      <header className="mt-5 flex items-start gap-3">
        <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary-subtle text-primary-text">
          <Icon icon={icon} size={22} />
        </span>
        <div>
          <h1 className="text-[28px] font-bold leading-9">{title}</h1>
          <p className="mt-0.5 text-sm text-text-secondary">{subtitle}</p>
        </div>
      </header>

      <p className="mt-4 text-xs text-text-subtle">Last updated: {LAST_UPDATED}</p>

      <div className="mt-8 space-y-8">{children}</div>

      <div className="mt-10 rounded-xl bg-primary-subtle p-4 text-sm text-text">
        Questions about this page? Contact us at{' '}
        <a href={`mailto:${contact}`} className="font-semibold text-primary-text hover:underline">
          {contact}
        </a>
        .
      </div>

      <div className="mt-6 flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-border pt-4 text-sm">
        <Link to="/terms" className="text-primary-text hover:underline">
          Terms of Service
        </Link>
        <Link to="/privacy" className="text-primary-text hover:underline">
          Privacy Policy
        </Link>
      </div>
    </article>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section>
      <h2 className="text-lg font-semibold text-text">{title}</h2>
      <div className="mt-2 space-y-3 text-sm leading-6 text-text-secondary">{children}</div>
    </section>
  );
}

function List({ items }: { items: ReactNode[] }) {
  return (
    <ul className="list-disc space-y-1.5 pl-5">
      {items.map((item, index) => (
        <li key={index}>{item}</li>
      ))}
    </ul>
  );
}

export function TermsPage() {
  return (
    <LegalLayout
      title="Terms of Service"
      subtitle="The rules for using FairFold as a candidate or an employer."
      icon={Scale}
      contact="legal@fairfold.app"
    >
      <p className="text-sm leading-6 text-text-secondary">
        These terms are an agreement between you and FairFold. By creating an account or using the
        platform, you accept them. If you use FairFold on behalf of an employer, you confirm that you
        have authority to accept these terms for that organisation.
      </p>

      <Section title="1. Your account">
        <List
          items={[
            'Give accurate, current information and keep it up to date.',
            'Keep your password and multi-factor authentication codes private. You are responsible for activity on your account.',
            'One person per account. Do not share an account or impersonate someone else.',
            'Tell us promptly if you believe your account has been compromised.',
          ]}
        />
      </Section>

      <Section title="2. Candidates">
        <List
          items={[
            'Your profile, work history and resume must describe your own real experience. Do not claim qualifications or skills you do not have.',
            'When you apply, the employer receives your application with your identity hidden. Your name and contact details are revealed only after you are shortlisted.',
            'Screening scores and skill assessments are advisory. They help employers compare applications, but a person makes the final decision. Applying does not guarantee an interview or a job.',
            'Assessments must be completed honestly and without help from other people or tools unless the assessment says otherwise. Focus signals are recorded only when you consent.',
            'Practice answers are private, are not shared with employers and never affect your applications or ranking.',
          ]}
        />
      </Section>

      <Section title="3. Employers">
        <List
          items={[
            'Job posts must be lawful, accurate and free from discriminatory requirements. Show the salary as required by the platform.',
            'Use candidate information only to assess and manage applications for the role the candidate applied to.',
            'Do not attempt to identify a candidate before shortlisting, and do not contact candidates outside the platform before their identity is revealed.',
            'Review AI-assisted scores and evidence yourself. Do not reject candidates automatically or rely on a score alone. Record written reasons for overrides as required by the product.',
            'Only invite team members who need access to hiring data, and remove access when it is no longer needed.',
          ]}
        />
      </Section>

      <Section title="4. Acceptable use">
        <p>You must not:</p>
        <List
          items={[
            'break any law or infringe the rights of others;',
            'harass, threaten, discriminate against or deceive other users;',
            'scrape, copy or resell data from the platform without permission;',
            'upload malware, attempt to gain unauthorised access, probe or disrupt the service;',
            'use the platform to send spam or unrelated marketing;',
            'submit sensitive information that is not needed for the hiring process.',
          ]}
        />
      </Section>

      <Section title="5. AI-assisted features">
        <p>
          FairFold uses AI to extract resume details, screen applications, generate assessment
          questions, summarise interview packs and grade practice answers. AI output can be
          incomplete or wrong. Scores are advisory, evidence quotes are shown where available, and
          rule-based results are labelled. A qualified person must review the output before making a
          hiring decision.
        </p>
        <p>
          Personal identifiers are removed before resume content is sent to the AI provider. Practice
          answers are anonymised before grading. Do not include information in free-text fields that
          you do not want processed by an AI provider.
        </p>
      </Section>

      <Section title="6. Messages and content">
        <p>
          Messages are visible to the participants in the hiring thread. Keep communication
          professional and relevant to the role. We may review content where needed to investigate
          abuse, security incidents or legal obligations. You keep ownership of the content you
          submit and grant us the licence needed to operate the service.
        </p>
      </Section>

      <Section title="7. Plans, billing and cancellation">
        <List
          items={[
            'Paid employer plans are billed through our payment processor. Prices, limits and included features are shown before checkout.',
            'Subscriptions renew until cancelled. You can cancel from the billing page; access continues until the end of the paid period.',
            'Quotas such as active jobs or screening runs are part of your plan and may be enforced by the platform.',
            'Except where the law requires otherwise, payments are non-refundable. Taxes may be added where applicable.',
          ]}
        />
      </Section>

      <Section title="8. Availability and changes to the service">
        <p>
          We work to keep FairFold available, but the service may be interrupted for maintenance,
          security or reasons outside our control. We may add, change or remove features. We will
          give reasonable notice of material changes that affect paid plans.
        </p>
      </Section>

      <Section title="9. Suspension and termination">
        <p>
          We may suspend or close an account that breaches these terms, creates risk for other users
          or is required by law. You may stop using FairFold at any time. On closure, personal data is
          handled as described in the Privacy Policy.
        </p>
      </Section>

      <Section title="10. Disclaimers and liability">
        <p>
          FairFold is provided &quot;as is&quot;. We do not guarantee that a candidate will be hired,
          that an employer will fill a role, or that AI output will be error-free. To the extent
          permitted by law, our total liability arising from the service is limited to the amount you
          paid us in the twelve months before the claim. Nothing in these terms excludes rights that
          cannot legally be excluded, including mandatory consumer rights.
        </p>
      </Section>

      <Section title="11. Changes to these terms">
        <p>
          We may update these terms as the product or the law changes. We will post the new version
          here and update the date above. If a change is material, we will notify you before it takes
          effect. Continuing to use FairFold after the effective date means you accept the updated
          terms.
        </p>
      </Section>

      <Section title="12. Contact">
        <p>
          For questions about these terms, contact{' '}
          <a href="mailto:legal@fairfold.app" className="text-primary-text hover:underline">
            legal@fairfold.app
          </a>
          . For privacy requests, see the Privacy Policy.
        </p>
      </Section>
    </LegalLayout>
  );
}

export function PrivacyPage() {
  return (
    <LegalLayout
      title="Privacy Policy"
      subtitle="What FairFold collects, why we collect it, and the choices you have."
      icon={ShieldCheck}
      contact="privacy@fairfold.app"
    >
      <p className="text-sm leading-6 text-text-secondary">
        This policy explains how FairFold handles personal data when you use our website, portals and
        hiring features. It applies to candidates, employers, interviewers, administrators and
        visitors to public job pages.
      </p>

      <Section title="1. Information we collect">
        <p>We collect the information you provide and the information generated by using the service:</p>
        <List
          items={[
            <>
              <strong className="font-semibold text-text">Account:</strong> name, email address, role,
              password hash and, if enabled, multi-factor authentication secrets.
            </>,
            <>
              <strong className="font-semibold text-text">Profile and resume:</strong> contact details,
              location, photo, skills, work history, certifications, uploaded resume files and the text
              extracted from them.
            </>,
            <>
              <strong className="font-semibold text-text">Applications:</strong> the jobs you apply to,
              application status, claims, screening scores, evidence quotes and employer decisions.
            </>,
            <>
              <strong className="font-semibold text-text">Assessments:</strong> answers, results and, only
              with your consent, focus signals such as a hidden tab or lost window focus.
            </>,
            <>
              <strong className="font-semibold text-text">Practice:</strong> questions you answer, your
              answers, AI feedback and helpfulness ratings. Practice data is kept separate from
              applications.
            </>,
            <>
              <strong className="font-semibold text-text">Messages:</strong> message content, participants,
              timestamps and read status.
            </>,
            <>
              <strong className="font-semibold text-text">Billing:</strong> plan, subscription status and
              payment-processor identifiers. We do not store full card numbers.
            </>,
            <>
              <strong className="font-semibold text-text">Usage and security:</strong> audit events,
              AI-call metadata (model, token counts, cost and status) and technical logs needed to run
              and protect the service.
            </>,
          ]}
        />
      </Section>

      <Section title="2. How we use information">
        <List
          items={[
            'create and secure accounts, sign you in and enforce role permissions;',
            'run hiring workflows: job posts, applications, screening, shortlisting, interviews and messages;',
            'generate explainable screening results, assessments, interview packs and practice feedback;',
            'process payments, manage plans and enforce plan limits;',
            'send transactional notifications and respond to support requests;',
            'detect abuse, investigate security incidents and maintain audit records;',
            'meet legal obligations and improve the reliability of the service.',
          ]}
        />
      </Section>

      <Section title="3. Legal bases">
        <p>
          Where data-protection law requires a legal basis, we rely on: performance of our contract
          with you; our legitimate interests in operating a fair and secure hiring platform; your
          consent for optional processing such as focus signals; and compliance with legal
          obligations. You can withdraw consent at any time without affecting processing that already
          took place.
        </p>
      </Section>

      <Section title="4. AI processing and anonymisation">
        <List
          items={[
            'Before resume content is sent to the screening AI provider, known identifiers and contact details are removed and the text is scanned for remaining identifiers.',
            'Screening results are returned with quotes from the anonymised text. Placeholders are restored only in the stored evidence shown to the employer after shortlisting.',
            'Practice answers are anonymised before grading. Practice is private and is never linked to a job or an application.',
            'AI outputs can be inaccurate. They are advisory, and a human makes the final hiring decision.',
          ]}
        />
      </Section>

      <Section title="5. Who can see your information">
        <List
          items={[
            'You can see and manage your own profile, applications, assessments and practice history.',
            'Employers see your application without your identity until you are shortlisted. After shortlisting, the hiring team for that role can see your identity, profile, resume and messages.',
            'Interviewers see only the interviews and packs they are assigned to.',
            'Administrators can access data when needed for support, content management, security or legal compliance. Admin actions are recorded in the audit log.',
            'We do not sell personal data or use it for advertising.',
          ]}
        />
      </Section>

      <Section title="6. Sharing with service providers">
        <p>
          We share data with providers that help us run FairFold, including AI processing, payment
          processing, hosting, email delivery and error monitoring. They receive only what they need
          and are bound by confidentiality and data-protection terms. We may also disclose data when
          required by law, to protect users, or as part of a merger or acquisition with appropriate
          safeguards.
        </p>
      </Section>

      <Section title="7. Cookies and local storage">
        <List
          items={[
            'A secure, HTTP-only cookie holds your refresh token so you can stay signed in. It is not readable by page scripts.',
            'Local storage remembers your light or dark theme choice.',
            'We do not use advertising or cross-site tracking cookies.',
          ]}
        />
      </Section>

      <Section title="8. Retention">
        <p>
          We keep account and hiring data while your account is active and for as long as needed to
          provide the service, resolve disputes and meet legal, tax and audit obligations. When data
          is no longer needed, we delete or anonymise it. Some records, such as audit logs and
          de-identified analytics, may be kept longer. Backup copies are removed on their normal
          rotation cycle.
        </p>
      </Section>

      <Section title="9. Security">
        <p>
          We use passwords hashed with BCrypt, short-lived access tokens, HTTP-only refresh cookies,
          optional multi-factor authentication, role-based access control and audit logging. No
          system is completely secure, so please use a strong unique password and enable multi-factor
          authentication where available.
        </p>
      </Section>

      <Section title="10. Your rights and choices">
        <List
          items={[
            'Access and export a copy of your data from the Account page.',
            'Correct inaccurate profile, resume or account information in the relevant page.',
            'Ask us to delete your account and associated personal data, subject to legal retention duties.',
            'Object to or ask us to restrict certain processing, and withdraw consent for optional features.',
            'Ask for a portable copy of data you provided, where the law provides that right.',
            'Complain to your local data-protection authority if you are not satisfied with our response.',
          ]}
        />
        <p>
          Candidates should contact the employer directly for requests about a specific application.
          We will help where the law requires.
        </p>
      </Section>

      <Section title="11. International transfers">
        <p>
          FairFold and its providers may process data in countries other than yours. Where we transfer
          personal data across borders, we use appropriate safeguards required by applicable law, such
          as standard contractual clauses.
        </p>
      </Section>

      <Section title="12. Children">
        <p>
          FairFold is for people who are old enough to work and enter into a contract. It is not
          intended for children under 16, and we do not knowingly collect their personal data. If you
          believe a child has created an account, contact us so we can remove it.
        </p>
      </Section>

      <Section title="13. Changes to this policy">
        <p>
          We may update this policy when the service or the law changes. The new version will be
          posted here with a new date. If a change materially affects how we use your data, we will
          notify you before it takes effect.
        </p>
      </Section>
    </LegalLayout>
  );
}

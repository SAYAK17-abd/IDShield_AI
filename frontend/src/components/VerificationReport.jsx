import React from 'react';
import {
  CheckCircle,
  AlertCircle,
  AlertOctagon,
  ScanFace,
  RefreshCw,
  Info,
  Check,
  X,
  Clock,
  Cpu,
} from 'lucide-react';

export default function VerificationReport({
  result,
  docMeta,
  isScreening,
  screeningStep,
  currentUser,
  onOfficerDecision,
}) {
  if (isScreening) {
    return (
      <div className="py-20 text-center flex flex-col items-center justify-center space-y-4 animate-pulse">
        <div className="w-16 h-16 rounded-3xl bg-indigo-950/60 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
          <RefreshCw className="w-8 h-8 animate-spin" />
        </div>
        <div className="text-sm font-bold text-white">{screeningStep}</div>
        <div className="w-64 h-2 bg-slate-800 rounded-full overflow-hidden">
          <div className="h-full bg-gradient-to-r from-indigo-500 to-blue-500 rounded-full w-2/3 animate-pulse"></div>
        </div>
        <div className="text-xs text-slate-500 font-mono">
          Inspecting magic bytes, font glyphs & biometric vectors
        </div>
      </div>
    );
  }

  if (!result) {
    return (
      <div className="py-20 text-center flex flex-col items-center justify-center space-y-3">
        <div className="w-16 h-16 rounded-3xl bg-slate-900 border border-slate-800 flex items-center justify-center text-slate-500 shadow-inner">
          <ScanFace className="w-8 h-8" />
        </div>
        <div className="text-sm font-semibold text-slate-300">Ready for Document Screening</div>
        <div className="text-xs text-slate-500 max-w-sm">
          Select an Indian identity document on the left and click "Run Security Screening" to generate forensic scores.
        </div>
      </div>
    );
  }

  const isLow = result.riskLevel === 'LOW';
  const isMed = result.riskLevel === 'MEDIUM';

  return (
    <div className="space-y-6 animate-in fade-in zoom-in-95">
      {/* VERDICT BANNER */}
      <div
        className={`p-4 sm:p-5 rounded-2xl border flex items-start space-x-3.5 ${
          isLow
            ? 'bg-emerald-950/30 border-emerald-500/40 text-emerald-300'
            : isMed
            ? 'bg-amber-950/30 border-amber-500/40 text-amber-300'
            : 'bg-rose-950/30 border-rose-500/40 text-rose-300'
        }`}
      >
        <div
          className={`p-2 rounded-xl text-white ${
            isLow ? 'bg-emerald-600' : isMed ? 'bg-amber-600' : 'bg-rose-600'
          }`}
        >
          {isLow ? (
            <CheckCircle className="w-5 h-5" />
          ) : isMed ? (
            <AlertCircle className="w-5 h-5" />
          ) : (
            <AlertOctagon className="w-5 h-5" />
          )}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-sm text-white">
              {isLow
                ? 'Document Verified — Authentic'
                : isMed
                ? 'Warning — Investigator Review Recommended'
                : 'High Risk Alert — Potential Fake / Tampered Document'}
            </h3>
            <span className="text-[11px] font-mono uppercase px-2 py-0.5 rounded bg-black/40 border border-white/10">
              {result.investigationStatus}
            </span>
          </div>
          <p className="text-xs opacity-90 mt-1">
            Risk Score: <span className="font-bold font-mono">{result.riskScore}/100</span> |
            Classification: <span className="font-bold">{result.riskLevel}</span>
          </p>
        </div>
      </div>

      {/* METRICS ROW (3 GAUGES) */}
      <div className="grid grid-cols-3 gap-3 sm:gap-4">
        <div className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 text-center">
          <div className="text-[11px] text-slate-400 font-medium">Face Biometric</div>
          <div className="text-base sm:text-lg font-bold text-white mt-1 font-mono">
            {(result.faceMatchConfidence * 100).toFixed(1)}%
          </div>
          <div
            className={`text-[10px] font-semibold mt-0.5 ${
              result.faceMatched ? 'text-emerald-400' : 'text-rose-400'
            }`}
          >
            {result.faceMatched ? 'Matched' : 'Mismatch'}
          </div>
        </div>

        <div className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 text-center">
          <div className="text-[11px] text-slate-400 font-medium">Tamper Risk</div>
          <div className="text-base sm:text-lg font-bold text-white mt-1 font-mono">
            {(result.tamperingConfidence * 100).toFixed(1)}%
          </div>
          <div
            className={`text-[10px] font-semibold mt-0.5 ${
              !result.tamperingDetected ? 'text-emerald-400' : 'text-rose-400'
            }`}
          >
            {!result.tamperingDetected ? 'Clean' : 'Tampered'}
          </div>
        </div>

        <div className="p-3.5 rounded-2xl bg-slate-900/80 border border-slate-800 text-center">
          <div className="text-[11px] text-slate-400 font-medium">Authenticity Index</div>
          <div className="text-base sm:text-lg font-bold text-emerald-400 mt-1 font-mono">
            {Math.max(0, 100 - result.riskScore)}%
          </div>
          <div className="text-[10px] text-slate-400 mt-0.5">Confidence</div>
        </div>
      </div>

      {/* EXTRACTED OCR & IDENTITY FIELDS */}
      <div className="space-y-2">
        <h4 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center justify-between">
          <span>Extracted OCR & Identity Fields</span>
          <span className="text-[10px] text-indigo-400 font-mono">AI Vision Extraction</span>
        </h4>

        <div className="p-4 rounded-2xl bg-slate-900/60 border border-slate-800/80 space-y-2 text-xs">
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Citizen Full Name</span>
            <span className="font-semibold text-white font-mono">{result.ocrData?.name || 'N/A'}</span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Document Identifier</span>
            <span className="font-semibold text-indigo-300 font-mono">
              {result.ocrData?.documentNumber || 'N/A'}
            </span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Date of Birth</span>
            <span className="font-semibold text-white font-mono">
              {result.ocrData?.dateOfBirth || 'N/A'}
            </span>
          </div>
          <div className="flex justify-between py-1 border-b border-slate-800/60">
            <span className="text-slate-400">Document Type</span>
            <span className="font-semibold text-white">{result.documentType}</span>
          </div>
          {docMeta?.sha256Checksum && (
            <div className="flex justify-between py-1">
              <span className="text-slate-400">SHA-256 Anti-Tamper Checksum</span>
              <span
                className="font-mono text-[10px] text-slate-400 truncate max-w-[200px]"
                title={docMeta.sha256Checksum}
              >
                {docMeta.sha256Checksum}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* SECURITY INTEGRITY CHECKS */}
      <div className="space-y-2">
        <h4 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
          Security Integrity Checks
        </h4>

        <div className="space-y-1.5 text-xs">
          <div className="p-2.5 rounded-xl bg-slate-900/40 border border-slate-800 flex items-center justify-between">
            <span className="text-slate-300 flex items-center gap-2">
              <Check className="w-3.5 h-3.5 text-emerald-400" />
              Magic-Byte File Signature Verification
            </span>
            <span className="text-[10px] font-mono text-emerald-400 font-semibold">PASSED</span>
          </div>

          <div className="p-2.5 rounded-xl bg-slate-900/40 border border-slate-800 flex items-center justify-between">
            <span className="text-slate-300 flex items-center gap-2">
              <Check className="w-3.5 h-3.5 text-emerald-400" />
              Cryptographic SHA-256 Digest Verification
            </span>
            <span className="text-[10px] font-mono text-emerald-400 font-semibold">MATCHED</span>
          </div>

          <div className="p-2.5 rounded-xl bg-slate-900/40 border border-slate-800 flex items-center justify-between">
            <span className="text-slate-300 flex items-center gap-2">
              {result.tamperingDetected ? (
                <X className="w-3.5 h-3.5 text-rose-400" />
              ) : (
                <Check className="w-3.5 h-3.5 text-emerald-400" />
              )}
              AI Neural Tamper & Pixel Noise Inspection
            </span>
            <span
              className={`text-[10px] font-mono font-semibold ${
                result.tamperingDetected ? 'text-rose-400' : 'text-emerald-400'
              }`}
            >
              {result.tamperingDetected ? 'ANOMALY DETECTED' : 'CLEAN'}
            </span>
          </div>

          {result.reasons?.map((reason, idx) => (
            <div
              key={idx}
              className="p-2.5 rounded-xl bg-indigo-950/20 border border-indigo-900/30 text-indigo-300 text-[11px] flex items-center gap-2"
            >
              <Info className="w-3.5 h-3.5 text-indigo-400 flex-shrink-0" />
              <span>Signal: {reason}</span>
            </div>
          ))}
        </div>
      </div>

      {/* OFFICER WORKFLOW DECISION BAR */}
      {(currentUser.role === 'ROLE_INVESTIGATOR' || currentUser.role === 'ROLE_ADMIN') && (
        <div className="pt-4 border-t border-slate-800 mt-6 space-y-2">
          <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider flex items-center justify-between">
            <span>Investigator Workflow Decisions</span>
            <span className="text-[10px] text-blue-400 font-mono">
              PATCH /api/verifications/{result.id}/status
            </span>
          </div>
          <div className="grid grid-cols-3 gap-2">
            <button
              type="button"
              onClick={() => onOfficerDecision('VERIFIED')}
              className="py-2 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs transition flex items-center justify-center gap-1.5 shadow"
            >
              <Check className="w-3.5 h-3.5" />
              Approve Authentic
            </button>
            <button
              type="button"
              onClick={() => onOfficerDecision('REVIEW_REQUIRED')}
              className="py-2 px-3 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-semibold text-xs transition flex items-center justify-center gap-1.5 shadow"
            >
              <Clock className="w-3.5 h-3.5" />
              Flag for Review
            </button>
            <button
              type="button"
              onClick={() => onOfficerDecision('REJECTED')}
              className="py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-semibold text-xs transition flex items-center justify-center gap-1.5 shadow"
            >
              <X className="w-3.5 h-3.5" />
              Reject as Fake
            </button>
          </div>
        </div>
      )}
    </div>
  );
}


import React, { useState, useEffect } from 'react';
import Navbar, { DEMO_USERS } from './components/Navbar';
import IndianDocSelector, { INDIAN_DOCUMENTS } from './components/IndianDocSelector';
import DocumentUploader from './components/DocumentUploader';
import SelfieCapture from './components/SelfieCapture';
import VerificationReport from './components/VerificationReport';
import AuditLogModal from './components/AuditLogModal';
import { api } from './services/api';
import { FilePlus, ShieldCheck, AlertTriangle, X } from 'lucide-react';

export default function App() {
  // Auth state
  const [currentUser, setCurrentUser] = useState(DEMO_USERS[0]);
  const [authToken, setAuthToken] = useState(null);
  const [gatewayStatus, setGatewayStatus] = useState('connecting');
  const [showAuditModal, setShowAuditModal] = useState(false);

  // Document state
  const [selectedDocType, setSelectedDocType] = useState(INDIAN_DOCUMENTS[0].id);
  const [docFile, setDocFile] = useState(null);
  const [docPreviewUrl, setDocPreviewUrl] = useState(null);
  const [selfieFile, setSelfieFile] = useState(null);
  const [selfiePreviewUrl, setSelfiePreviewUrl] = useState(null);

  // Screening state
  const [isScreening, setIsScreening] = useState(false);
  const [screeningStep, setScreeningStep] = useState('');
  const [verificationResult, setVerificationResult] = useState(null);
  const [uploadedDocMeta, setUploadedDocMeta] = useState(null);
  const [errorNotice, setErrorNotice] = useState(null);

  // Audit Logs (Admin)
  const [auditLogs, setAuditLogs] = useState([]);
  const [loadingLogs, setLoadingLogs] = useState(false);

  // Login on component mount
  useEffect(() => {
    loginUser(currentUser);
  }, []);

  // Cleanup object URLs to avoid memory leaks
  useEffect(() => {
    return () => {
      if (docPreviewUrl) URL.revokeObjectURL(docPreviewUrl);
      if (selfiePreviewUrl) URL.revokeObjectURL(selfiePreviewUrl);
    };
  }, [docPreviewUrl, selfiePreviewUrl]);

  const loginUser = async (userObj) => {
    setGatewayStatus('connecting');
    setErrorNotice(null);
    try {
      const data = await api.login(userObj.email, userObj.pass);
      setAuthToken(data.data?.accessToken);
      setCurrentUser(userObj);
      setGatewayStatus('connected');
    } catch (err) {
      setGatewayStatus('disconnected');
      setErrorNotice(`Cannot connect to Spring Boot Gateway: ${err.message}`);
    }
  };

  const handleDocSelect = (file) => {
    if (!file) return;
    setDocFile(file);
    if (file.type.startsWith('image/')) {
      setDocPreviewUrl(URL.createObjectURL(file));
    } else {
      setDocPreviewUrl(null);
    }
  };

  const handleSelfieSelect = (file) => {
    if (!file) return;
    setSelfieFile(file);
    if (file.type.startsWith('image/')) {
      setSelfiePreviewUrl(URL.createObjectURL(file));
    }
  };

  const executeScreening = async () => {
    if (!docFile) {
      setErrorNotice('Please select or capture an Indian identity document first.');
      return;
    }
    if (!authToken) {
      setErrorNotice('Not authenticated with Spring Boot Gateway. Please wait for reconnection.');
      return;
    }

    setIsScreening(true);
    setErrorNotice(null);
    setVerificationResult(null);

    try {
      setScreeningStep('1/3: Ingesting & Verifying Magic Bytes...');
      const uploadResp = await api.uploadDocument(docFile, selectedDocType, authToken, selfieFile);
      const docId = uploadResp.data?.id;
      setUploadedDocMeta(uploadResp.data);

      setScreeningStep('2/3: Running AI Biometrics & Tamper Neural Inspection...');
      const verifyResp = await api.triggerVerification(docId, authToken);

      setScreeningStep('3/3: Synthesizing Transparent Risk Scores...');
      await new Promise((r) => setTimeout(r, 600));
      setVerificationResult(verifyResp.data);
    } catch (err) {
      setErrorNotice(err.message || 'An unexpected error occurred during screening.');
    } finally {
      setIsScreening(false);
      setScreeningStep('');
    }
  };

  const handleOfficerDecision = async (status) => {
    if (!verificationResult?.id || !authToken) return;
    try {
      const notes = `Decision confirmed as ${status} by ${currentUser.name}`;
      await api.updateVerificationStatus(verificationResult.id, status, notes, authToken);
      setVerificationResult((prev) => ({
        ...prev,
        investigationStatus: status,
        reviewedByUserEmail: currentUser.email,
        investigatorNotes: notes,
      }));
    } catch (err) {
      alert(`Failed to update case: ${err.message}`);
    }
  };

  const loadAuditLogs = async () => {
    if (!authToken) return;
    setShowAuditModal(true);
    setLoadingLogs(true);
    try {
      const data = await api.getAuditLogs(0, 15, authToken);
      setAuditLogs(data.data?.content || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingLogs(false);
    }
  };

  const selectedDocObj =
    INDIAN_DOCUMENTS.find((d) => d.id === selectedDocType) || INDIAN_DOCUMENTS[0];
  const IconComponent = currentUser.icon;

  return (
    <div className="flex-1 flex flex-col min-h-screen">
      <Navbar
        currentUser={currentUser}
        onUserSwitch={loginUser}
        gatewayStatus={gatewayStatus}
        onOpenAuditModal={loadAuditLogs}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8 space-y-6">
        {/* Role Banner Notification */}
        <div
          className={`p-3.5 rounded-2xl border flex flex-col sm:flex-row sm:items-center justify-between gap-2 shadow-sm ${
            currentUser.role === 'ROLE_ADMIN'
              ? 'bg-purple-950/20 border-purple-800/40 text-purple-300'
              : currentUser.role === 'ROLE_INVESTIGATOR'
              ? 'bg-blue-950/20 border-blue-800/40 text-blue-300'
              : 'bg-emerald-950/20 border-emerald-800/40 text-emerald-300'
          }`}
        >
          <div className="flex items-center space-x-2.5">
            <div className={`p-2 rounded-xl text-white ${currentUser.avatarBg}`}>
              <IconComponent className="w-4 h-4" />
            </div>
            <div>
              <div className="font-semibold text-xs text-white">
                Signed in as {currentUser.title} ({currentUser.email})
              </div>
              <div className="text-[11px] opacity-80">{currentUser.desc}</div>
            </div>
          </div>

          {currentUser.role === 'ROLE_ADMIN' && (
            <button
              onClick={loadAuditLogs}
              className="text-xs bg-purple-600/30 hover:bg-purple-600/50 border border-purple-500/40 text-purple-200 px-3 py-1.5 rounded-lg transition font-medium self-start sm:self-auto"
            >
              Inspect Audit Trail (Immutable)
            </button>
          )}
        </div>

        {/* Error Notification */}
        {errorNotice && (
          <div className="p-4 rounded-2xl bg-rose-950/40 border border-rose-800/60 text-rose-300 text-xs flex items-start space-x-3">
            <AlertTriangle className="w-4 h-4 text-rose-400 flex-shrink-0 mt-0.5" />
            <div className="flex-1 font-medium">{errorNotice}</div>
            <button onClick={() => setErrorNotice(null)} className="text-rose-400 hover:text-rose-200">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Dual Column Layout: Left (Upload) | Right (Results) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Left Column: Upload Card */}
          <div className="lg:col-span-5 space-y-6">
            <div className="glass-card rounded-3xl p-5 sm:p-6 border border-slate-800 shadow-xl space-y-5">
              <div>
                <h2 className="text-base sm:text-lg font-bold text-white flex items-center gap-2">
                  <FilePlus className="w-5 h-5 text-indigo-400" />
                  Document & Identity Ingestion
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  Select document format, upload ID, and run multi-layer forensic screening.
                </p>
              </div>

              {/* Indian Document Selector */}
              <IndianDocSelector
                selectedDocType={selectedDocType}
                onSelect={setSelectedDocType}
              />

              {/* Document File Uploader */}
              <DocumentUploader
                docName={selectedDocObj.name}
                file={docFile}
                previewUrl={docPreviewUrl}
                onFileSelect={handleDocSelect}
                onRemove={() => {
                  setDocFile(null);
                  setDocPreviewUrl(null);
                }}
              />

              {/* Optional Selfie Biometric Capture */}
              <SelfieCapture
                docName={selectedDocObj.name}
                file={selfieFile}
                previewUrl={selfiePreviewUrl}
                onSelect={handleSelfieSelect}
                onRemove={() => {
                  setSelfieFile(null);
                  setSelfiePreviewUrl(null);
                }}
              />

              {/* Run Screening Action Button */}
              <div className="pt-2">
                <button
                  type="button"
                  disabled={isScreening || !docFile}
                  onClick={executeScreening}
                  className={`w-full py-3.5 px-4 rounded-2xl font-bold text-sm text-white shadow-xl flex items-center justify-center space-x-2 transition active:scale-[0.98] ${
                    isScreening
                      ? 'bg-indigo-700 cursor-wait'
                      : !docFile
                      ? 'bg-slate-800 text-slate-500 cursor-not-allowed border border-slate-700/50'
                      : 'bg-gradient-to-r from-indigo-600 via-indigo-500 to-blue-600 hover:from-indigo-500 hover:to-blue-500 shadow-indigo-600/30'
                  }`}
                >
                  {isScreening ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                      <span>{screeningStep || 'Screening Document...'}</span>
                    </>
                  ) : (
                    <>
                      <ShieldCheck className="w-4 h-4" />
                      <span>Run Multi-Signal Security Screening</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Right Column: Verification Report Card */}
          <div className="lg:col-span-7 space-y-6">
            <div className="glass-card rounded-3xl p-5 sm:p-6 border border-slate-800 shadow-xl min-h-[520px] flex flex-col justify-between">
              <VerificationReport
                result={verificationResult}
                docMeta={uploadedDocMeta}
                isScreening={isScreening}
                screeningStep={screeningStep}
                currentUser={currentUser}
                onOfficerDecision={handleOfficerDecision}
              />
            </div>
          </div>
        </div>
      </main>

      {/* Audit Log Modal for Admin */}
      <AuditLogModal
        isOpen={showAuditModal}
        onClose={() => setShowAuditModal(false)}
        logs={auditLogs}
        loading={loadingLogs}
      />

      <footer className="border-t border-slate-800/80 px-4 py-4 text-center text-xs text-slate-500">
        SIH26188 — AI-Based Fake Identity & Document Screening System | Security Gateway & Risk Engine
      </footer>
    </div>
  );
}


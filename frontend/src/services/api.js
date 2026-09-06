/**
 * IDShield AI — API Client Service
 * Routes all requests strictly to the Spring Boot Security Gateway.
 */

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export const api = {
  // Authentication: Login & obtain JWT Bearer token
  async login(email, password) {
    const res = await fetch(`${BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Login failed (HTTP ${res.status})`);
    }
    return res.json();
  },

  // Document Management: Upload with multipart/form-data
  async uploadDocument(file, documentType, token, selfie = null) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('documentType', documentType);
    if (selfie) {
      formData.append('selfie', selfie);
    }

    const res = await fetch(`${BASE_URL}/api/documents/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Document upload rejected (HTTP ${res.status})`);
    }
    return res.json();
  },

  // Verification: Trigger AI screening pipeline & risk calculation
  async triggerVerification(documentId, token) {
    const res = await fetch(`${BASE_URL}/api/verifications/documents/${documentId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Verification screening failed (HTTP ${res.status})`);
    }
    return res.json();
  },

  // Verification: Update investigation case status (Officer/Admin only)
  async updateVerificationStatus(verificationId, status, notes, token) {
    const res = await fetch(`${BASE_URL}/api/verifications/${verificationId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        status,
        investigatorNotes: notes,
      }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Status update failed (HTTP ${res.status})`);
    }
    return res.json();
  },

  // Audit Trail: Retrieve immutable audit logs (Admin only)
  async getAuditLogs(page = 0, size = 20, token) {
    const res = await fetch(`${BASE_URL}/api/audit-logs?page=${page}&size=${size}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || `Audit logs fetch failed (HTTP ${res.status})`);
    }
    return res.json();
  },

  // System Health
  async getHealth() {
    try {
      const res = await fetch(`${BASE_URL}/actuator/health`);
      return res.ok;
    } catch {
      return false;
    }
  },
};


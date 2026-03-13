import React, { useState } from 'react';
import { register, login } from '../services/api';

const PASSWORD_RULES = [
  { test: v => v.length >= 12,              label: '12+ characters'              },
  { test: v => /[A-Z]/.test(v),             label: 'One uppercase letter'        },
  { test: v => /[a-z]/.test(v),             label: 'One lowercase letter'        },
  { test: v => /\d/.test(v),                label: 'One digit'                   },
  { test: v => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(v),
                                             label: 'One special character'       },
];

function PasswordStrength({ password }) {
  if (!password) return null;
  const passed = PASSWORD_RULES.filter(r => r.test(password)).length;
  const pct    = (passed / PASSWORD_RULES.length) * 100;
  const color  = passed <= 2 ? 'var(--red)' : passed <= 3 ? 'var(--amber)' : 'var(--green)';
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ height: 4, background: 'var(--dark-green)', marginBottom: 8 }}>
        <div style={{ height: '100%', width: `${pct}%`, background: color, transition: 'width 0.3s, background 0.3s' }} />
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2px 16px' }}>
        {PASSWORD_RULES.map(r => (
          <div key={r.label} style={{ fontSize: 10, color: r.test(password) ? 'var(--green)' : 'var(--dim-green)', letterSpacing: 1 }}>
            {r.test(password) ? '✓' : '○'} {r.label}
          </div>
        ))}
      </div>
    </div>
  );
}

export default function LoginScreen({ onLogin }) {
  const [mode,     setMode]     = useState('login'); // 'login' | 'register'
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState('');

  const allRulesPassed = PASSWORD_RULES.every(r => r.test(password));

  const handleSubmit = async () => {
    if (!username.trim() || !password) return;
    if (mode === 'register' && !allRulesPassed) {
      setError('Password does not meet requirements.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const tokens = mode === 'register'
        ? await register(username.trim(), password)
        : await login(username.trim(), password);
      // Fetch player profile after auth
      const { getMe, getMyPlayer } = await import('../services/api');
      const me     = await getMe();
      const player = await getMyPlayer();
      onLogin({ ...player, username: me.username });
    } catch (err) {
      const msg = err?.response?.data?.message || err?.response?.data?.error;
      // Map validation detail errors
      const details = err?.response?.data?.details;
      if (details?.password) {
        setError(`Password: ${details.password}`);
      } else {
        setError(msg || 'Connection failed. Check vault terminal.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fade-in" style={{ maxWidth: 480, margin: '50px auto' }}>
      <div className="panel">
        <div className="panel-label">VAULT ACCESS TERMINAL</div>

        <div style={{ marginBottom: 20, lineHeight: 1.8, fontSize: 12, color: 'var(--dim-green)' }}>
          <span style={{ color: 'var(--green)' }}>SYSTEM:</span> Welcome to the Knowledge Vault Initiative.<br />
          <span style={{ color: 'var(--green)' }}>SYSTEM:</span> {mode === 'login'
            ? 'Authenticate to access your profile.'
            : 'Register your intellect profile to begin.'}<br />
          <span style={{ color: 'var(--green)' }}>SYSTEM:</span> The wasteland rewards the learned.
        </div>

        {/* Mode toggle */}
        <div style={{ display: 'flex', marginBottom: 20, border: '1px solid var(--border)' }}>
          {['login', 'register'].map(m => (
            <button
              key={m}
              onClick={() => { setMode(m); setError(''); setPassword(''); }}
              style={{
                flex: 1, padding: '8px 0', background: mode === m ? 'rgba(57,255,20,0.1)' : 'transparent',
                border: 'none', borderRight: m === 'login' ? '1px solid var(--border)' : 'none',
                color: mode === m ? 'var(--green)' : 'var(--dim-green)',
                fontFamily: "'Share Tech Mono', monospace", fontSize: 12, letterSpacing: 2,
                cursor: 'pointer', textTransform: 'uppercase',
              }}
            >
              {m === 'login' ? '► LOGIN' : '► REGISTER'}
            </button>
          ))}
        </div>

        {/* Username */}
        <div style={{ marginBottom: 4, fontSize: 10, letterSpacing: 2, color: 'var(--dim-green)' }}>
          VAULT DWELLER DESIGNATION (3–20 chars, letters/digits/_/-)
        </div>
        <input
          type="text"
          placeholder="vault_dweller_01"
          value={username}
          onChange={e => setUsername(e.target.value)}
          maxLength={20}
          style={{ marginBottom: 14 }}
          autoComplete="username"
        />

        {/* Password */}
        <div style={{ marginBottom: 4, fontSize: 10, letterSpacing: 2, color: 'var(--dim-green)' }}>
          {mode === 'register' ? 'ACCESS CODE (see requirements below)' : 'ACCESS CODE'}
        </div>
        <div style={{ position: 'relative', marginBottom: 12 }}>
          <input
            type={showPass ? 'text' : 'password'}
            placeholder={mode === 'register' ? 'Min 12 chars, mixed case + symbols' : '••••••••••••'}
            value={password}
            onChange={e => setPassword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSubmit()}
            maxLength={72}
            style={{ paddingRight: 44 }}
            autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
          />
          <button
            onClick={() => setShowPass(s => !s)}
            style={{
              position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)',
              background: 'none', border: 'none', color: 'var(--dim-green)', cursor: 'pointer',
              fontFamily: "'Share Tech Mono', monospace", fontSize: 11,
            }}
          >
            {showPass ? 'HIDE' : 'SHOW'}
          </button>
        </div>

        {/* Password strength meter (register only) */}
        {mode === 'register' && <PasswordStrength password={password} />}

        {error && (
          <div style={{ color: 'var(--red)', fontSize: 11, marginBottom: 12, letterSpacing: 1 }}>
            ⚠ {error}
          </div>
        )}

        <button
          className="btn btn-amber"
          onClick={handleSubmit}
          disabled={loading || !username.trim() || !password || (mode === 'register' && !allRulesPassed)}
          style={{ width: '100%', fontSize: 13, padding: 12,
            opacity: (!username.trim() || !password || (mode === 'register' && !allRulesPassed)) ? 0.4 : 1 }}
        >
          {loading
            ? <span>AUTHENTICATING<span className="blink">_</span></span>
            : mode === 'login' ? '► ENTER THE VAULT' : '► REGISTER DWELLER'
          }
        </button>
      </div>

      <div style={{ textAlign: 'center', fontSize: 10, color: 'var(--dim)', letterSpacing: 2, marginTop: 12 }}>
        VAULT-TEC CORP. — "PREPARING FOR THE FUTURE, TODAY"
      </div>
    </div>
  );
}

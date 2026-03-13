import React, { useEffect, useState } from 'react';

export default function Header({ player, screen, onLeaderboard, onLogout }) {
  const [provider, setProvider] = useState('');

  useEffect(() => {
    fetch('/api/health')
      .then(r => r.json())
      .then(d => setProvider(d.aiProvider || ''))
      .catch(() => {});
  }, []);

  return (
    <div className="panel" style={{ textAlign: 'center', marginBottom: 20 }}>
      <div className="panel-label">◄ VAULT-TEC KNOWLEDGE INITIATIVE ►</div>

      <div style={{
        fontFamily: "'VT323', monospace",
        fontSize: 52,
        color: 'var(--green)',
        textShadow: '0 0 20px var(--green), 0 0 40px rgba(57,255,20,0.4)',
        letterSpacing: 6,
        lineHeight: 1,
      }}>
        KNOWLEDGE VAULT
      </div>

      <div style={{ fontSize: 10, color: 'var(--dim-green)', letterSpacing: 3, marginTop: 4 }}>
        S.K.I.L.L. SYSTEM v2.7.7 — SECURED
        {provider && (
          <span style={{ marginLeft: 12, color: 'var(--amber)' }}>
            ◄ AI: {provider.split(' ')[0].toUpperCase()} ►
          </span>
        )}
      </div>

      {player && (
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginTop: 14,
          paddingTop: 12,
          borderTop: '1px solid var(--border)',
          fontSize: 12,
          flexWrap: 'wrap',
          gap: 8,
        }}>
          <span>
            <span className="dim">DWELLER: </span>
            <span style={{ color: 'var(--amber)' }}>{player.username?.toUpperCase()}</span>
          </span>
          <span>
            <span className="dim">LVL </span>
            <span className="glow-text">{player.level}</span>
          </span>
          <span>
            <span className="dim">SCORE: </span>
            <span style={{ color: 'var(--amber)' }}>{(player.totalScore || 0).toLocaleString()}</span>
          </span>
          <span>
            <span className="dim">STREAK: </span>
            <span style={{ color: (player.currentStreak || 0) > 0 ? 'var(--amber)' : 'var(--dim-green)' }}>
              {player.currentStreak || 0}🔥
            </span>
          </span>
          <div style={{ display: 'flex', gap: 8 }}>
            {screen !== 'leaderboard' && (
              <button className="btn btn-dim" style={{ fontSize: 10, padding: '4px 10px' }} onClick={onLeaderboard}>
                RANKS
              </button>
            )}
            <button
              className="btn"
              style={{ fontSize: 10, padding: '4px 10px', borderColor: 'var(--red)', color: 'var(--red)' }}
              onClick={onLogout}
            >
              LOGOUT
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

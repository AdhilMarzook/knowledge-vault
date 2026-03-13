import React, { useEffect, useState } from 'react';
import { getLeaderboard } from '../services/api';

export default function LeaderboardScreen({ currentPlayer, onBack }) {
  const [leaders, setLeaders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getLeaderboard()
      .then(setLeaders)
      .finally(() => setLoading(false));
  }, []);

  const medals = ['◈', '◇', '△'];

  return (
    <div className="fade-in">
      <div className="panel" style={{ textAlign: 'center', marginBottom: 16 }}>
        <div className="panel-label">GLOBAL RANKINGS</div>
        <div style={{ fontFamily: "'VT323', monospace", fontSize: 36, letterSpacing: 4, color: 'var(--amber)',
          textShadow: '0 0 15px var(--amber)' }}>
          VAULT LEADERBOARD
        </div>
        <div style={{ fontSize: 10, color: 'var(--dim-green)', letterSpacing: 3 }}>
          TOP KNOWLEDGE DWELLERS — ALL VAULTS
        </div>
      </div>

      {loading ? (
        <div className="panel" style={{ textAlign: 'center', padding: 40 }}>
          <div className="dim">FETCHING RECORDS<span className="blink">_</span></div>
        </div>
      ) : leaders.length === 0 ? (
        <div className="panel" style={{ textAlign: 'center', padding: 40 }}>
          <div className="dim">NO RECORDS FOUND. BE THE FIRST.</div>
        </div>
      ) : (
        <div className="panel" style={{ marginBottom: 16 }}>
          <div className="panel-label">TOP DWELLERS</div>
          {/* Table Header */}
          <div style={{ display: 'grid', gridTemplateColumns: '40px 1fr 80px 80px 80px',
            gap: 12, fontSize: 10, color: 'var(--dim-green)', letterSpacing: 2,
            borderBottom: '1px solid var(--border)', paddingBottom: 8, marginBottom: 8 }}>
            <span>#</span>
            <span>DWELLER</span>
            <span style={{ textAlign: 'right' }}>LEVEL</span>
            <span style={{ textAlign: 'right' }}>SCORE</span>
            <span style={{ textAlign: 'right' }}>QUESTS</span>
          </div>

          {leaders.map((player, idx) => {
            const isCurrentPlayer = player.id === currentPlayer?.id;
            return (
              <div
                key={player.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '40px 1fr 80px 80px 80px',
                  gap: 12,
                  padding: '10px 0',
                  borderBottom: '1px solid var(--border)',
                  fontSize: 13,
                  background: isCurrentPlayer ? 'rgba(57,255,20,0.05)' : 'transparent',
                  color: isCurrentPlayer ? 'var(--green)' : idx < 3 ? 'var(--amber)' : 'var(--text)',
                  boxShadow: isCurrentPlayer ? 'inset 0 0 10px rgba(57,255,20,0.05)' : 'none',
                }}
              >
                <span style={{ fontFamily: "'VT323', monospace", fontSize: 20,
                  color: idx === 0 ? '#ffd700' : idx === 1 ? '#c0c0c0' : idx === 2 ? '#cd7f32' : 'var(--dim-green)' }}>
                  {idx < 3 ? medals[idx] : idx + 1}
                </span>
                <span>
                  {player.username.toUpperCase()}
                  {isCurrentPlayer && <span style={{ fontSize: 10, color: 'var(--dim-green)', marginLeft: 8 }}>◄ YOU</span>}
                </span>
                <span style={{ textAlign: 'right', fontFamily: "'VT323', monospace", fontSize: 20 }}>
                  {player.level}
                </span>
                <span style={{ textAlign: 'right' }}>
                  {player.totalScore.toLocaleString()}
                </span>
                <span style={{ textAlign: 'right', color: 'var(--dim-green)' }}>
                  {player.questsCompleted}
                </span>
              </div>
            );
          })}
        </div>
      )}

      <button className="btn" onClick={onBack} style={{ width: '100%' }}>
        ◄ RETURN TO VAULT
      </button>
    </div>
  );
}

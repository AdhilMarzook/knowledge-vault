import React, { useEffect, useState } from 'react';

export default function ResultScreen({ result, quest, onNext, onBack, onLeaderboard }) {
  const [showDetails, setShowDetails] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setShowDetails(true), 600);
    return () => clearTimeout(timer);
  }, []);

  const isCorrect = result.correct;

  return (
    <div className="fade-in">
      {/* Result Banner */}
      <div className="panel" style={{
        textAlign: 'center',
        borderColor: isCorrect ? 'var(--green)' : 'var(--red)',
        boxShadow: isCorrect
          ? '0 0 30px rgba(57,255,20,0.25)'
          : '0 0 30px rgba(255,60,0,0.25)',
        marginBottom: 16,
        padding: 30,
      }}>
        <div style={{
          fontFamily: "'VT323', monospace",
          fontSize: 56,
          color: isCorrect ? 'var(--green)' : 'var(--red)',
          textShadow: isCorrect
            ? '0 0 20px var(--green), 0 0 40px rgba(57,255,20,0.5)'
            : '0 0 20px var(--red)',
          letterSpacing: 4,
          lineHeight: 1,
        }}>
          {isCorrect ? '✓ CORRECT' : '✗ WRONG'}
        </div>
        <div style={{ fontSize: 12, color: isCorrect ? 'var(--dim-green)' : '#7a2010', marginTop: 6, letterSpacing: 3 }}>
          {isCorrect ? 'INTELLIGENCE CHECK PASSED' : 'INTELLIGENCE CHECK FAILED'}
        </div>
      </div>

      {showDetails && (
        <>
          {/* NPC Reaction */}
          <div className="panel fade-in" style={{
            borderColor: isCorrect ? 'var(--amber)' : 'var(--dim)',
            marginBottom: 16
          }}>
            <div className="panel-label" style={{ color: 'var(--amber)' }}>
              NPC REACTION — {quest.npcName?.toUpperCase()}
            </div>
            <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
              <div style={{
                width: 52, height: 52, border: `1px solid ${isCorrect ? 'var(--amber)' : 'var(--dim-green)'}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 28, flexShrink: 0,
              }}>
                {isCorrect ? '😤' : '😒'}
              </div>
              <div style={{ fontSize: 13, color: isCorrect ? '#ddb060' : 'var(--dim-green)', lineHeight: 1.6, fontStyle: 'italic' }}>
                "{result.npcReaction}"
              </div>
            </div>
          </div>

          {/* Explanation */}
          <div className="panel fade-in" style={{ marginBottom: 16 }}>
            <div className="panel-label">KNOWLEDGE LOG</div>
            <div style={{ fontSize: 13, lineHeight: 1.7, color: 'var(--dim-green)' }}>
              <span style={{ color: 'var(--green)' }}>CORRECT ANSWER: </span>
              {quest.choices[quest.correctIndex]}
            </div>
            <div style={{ fontSize: 12, lineHeight: 1.7, color: 'var(--dim-green)', marginTop: 8 }}>
              <span style={{ color: 'var(--green)' }}>EXPLANATION: </span>
              {result.explanation}
            </div>
          </div>

          {/* Stats */}
          {isCorrect && (
            <div className="panel fade-in" style={{ marginBottom: 16 }}>
              <div className="panel-label">REWARDS EARNED</div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 12, textAlign: 'center' }}>
                <div>
                  <div style={{ fontSize: 10, color: 'var(--dim-green)', letterSpacing: 2, marginBottom: 4 }}>XP EARNED</div>
                  <div style={{ fontFamily: "'VT323', monospace", fontSize: 28, color: 'var(--amber)', textShadow: '0 0 10px var(--amber)' }}>
                    +{result.xpEarned}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: 10, color: 'var(--dim-green)', letterSpacing: 2, marginBottom: 4 }}>SCORE</div>
                  <div style={{ fontFamily: "'VT323', monospace", fontSize: 28, color: 'var(--amber)', textShadow: '0 0 10px var(--amber)' }}>
                    +{result.scoreEarned}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: 10, color: 'var(--dim-green)', letterSpacing: 2, marginBottom: 4 }}>SKILL LEVEL</div>
                  <div style={{ fontFamily: "'VT323', monospace", fontSize: 28, color: 'var(--green)', textShadow: '0 0 10px var(--green)' }}>
                    {result.newSkillLevel}
                  </div>
                </div>
              </div>

              {result.leveledUp && (
                <div style={{
                  marginTop: 12, padding: 12, border: '1px solid var(--amber)',
                  textAlign: 'center', background: 'rgba(255,176,0,0.05)',
                  animation: 'fadeIn 0.5s ease',
                }}>
                  <div style={{ fontFamily: "'VT323', monospace", fontSize: 28, color: 'var(--amber)', textShadow: '0 0 15px var(--amber)' }}>
                    ★ VAULT DWELLER LEVEL UP! ★
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--amber)', letterSpacing: 2 }}>
                    REACHED LEVEL {result.newPlayerLevel}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Actions */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10 }}>
            <button className="btn" onClick={onNext} style={{ fontSize: 13 }}>
              ► NEXT QUEST
            </button>
            <button className="btn btn-dim" onClick={onBack}>
              SKILL SELECT
            </button>
            <button className="btn btn-dim" onClick={onLeaderboard}>
              LEADERBOARD
            </button>
          </div>
        </>
      )}
    </div>
  );
}

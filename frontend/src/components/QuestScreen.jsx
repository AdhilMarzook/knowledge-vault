import React, { useState, useEffect } from 'react';
import { generateQuest, submitAnswer } from '../services/api';

const DIFFICULTY_LABEL = ['','TRIVIAL','EASY','MODERATE','MODERATE','AVERAGE',
  'CHALLENGING','HARD','HARD','EXPERT','MASTER'];

export default function QuestScreen({ player, skill, onResult, onBack }) {
  const [quest,          setQuest]          = useState(null);
  const [loading,        setLoading]        = useState(true);
  const [selected,       setSelected]       = useState(null);
  const [submitting,     setSubmitting]     = useState(false);
  const [error,          setError]          = useState('');
  const [typedQuestion,  setTypedQuestion]  = useState('');

  useEffect(() => { fetchQuest(); }, []);

  useEffect(() => {
    if (!quest) return;
    let i = 0;
    setTypedQuestion('');
    const iv = setInterval(() => {
      setTypedQuestion(quest.question.slice(0, i));
      i++;
      if (i > quest.question.length) clearInterval(iv);
    }, 22);
    return () => clearInterval(iv);
  }, [quest]);

  const fetchQuest = async () => {
    setLoading(true); setSelected(null); setError('');
    try {
      // New API: skill only (playerId comes from JWT on backend)
      const q = await generateQuest(skill);
      setQuest(q);
    } catch (e) {
      setError('TRANSMISSION FAILED. RETRY.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (selected === null || submitting) return;
    setSubmitting(true);
    try {
      // New API: questId + selectedIndex (playerId from JWT on backend)
      const result = await submitAnswer(quest.id, selected);
      onResult(result, quest);
    } catch {
      setError('SUBMISSION ERROR. TRY AGAIN.');
      setSubmitting(false);
    }
  };

  if (loading) return (
    <div className="panel fade-in" style={{ textAlign: 'center', padding: 60 }}>
      <div style={{ fontFamily: "'VT323', monospace", fontSize: 28, color: 'var(--dim-green)' }}>
        AI QUEST ENGINE GENERATING<span className="blink">_</span>
      </div>
      <div style={{ fontSize: 11, color: 'var(--dim)', marginTop: 12, letterSpacing: 2 }}>
        CALIBRATING DIFFICULTY TO SKILL LEVEL...
      </div>
    </div>
  );

  if (error) return (
    <div className="panel fade-in" style={{ textAlign: 'center', padding: 40 }}>
      <div style={{ color: 'var(--red)', marginBottom: 16 }}>⚠ {error}</div>
      <button className="btn" onClick={fetchQuest}>RETRY</button>
      <button className="btn btn-dim" onClick={onBack} style={{ marginLeft: 12 }}>BACK</button>
    </div>
  );

  return (
    <div className="fade-in">
      {/* NPC Dialogue */}
      <div className="panel" style={{ borderColor: 'var(--amber)', marginBottom: 16 }}>
        <div className="panel-label" style={{ color: 'var(--amber)' }}>
          NPC DIALOGUE — {quest?.npcName?.toUpperCase()}
        </div>
        <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
          <div style={{
            width: 52, height: 52, border: '1px solid var(--amber)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 28, flexShrink: 0, background: 'rgba(255,176,0,0.05)',
          }}>🤖</div>
          <div>
            <div style={{ fontSize: 11, color: 'var(--amber)', letterSpacing: 2, marginBottom: 4 }}>
              {quest?.npcName?.toUpperCase()}
            </div>
            <div style={{ fontSize: 13, color: '#ddb060', lineHeight: 1.6 }}>
              "{quest?.npcDialogue}"
            </div>
          </div>
        </div>
      </div>

      {/* Meta */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12, fontSize: 11, color: 'var(--dim-green)', flexWrap: 'wrap', gap: 6 }}>
        <span>► SKILL: <span style={{ color: 'var(--green)' }}>{skill?.toUpperCase()}</span></span>
        <span>DIFFICULTY: <span style={{ color: quest?.difficulty >= 7 ? 'var(--red)' : quest?.difficulty >= 4 ? 'var(--amber)' : 'var(--green)' }}>
          {DIFFICULTY_LABEL[quest?.difficulty]} ({quest?.difficulty}/10)
        </span></span>
        <span>XP: <span style={{ color: 'var(--amber)' }}>+{quest?.xpReward}</span></span>
        <span>SCORE: <span style={{ color: 'var(--amber)' }}>+{quest?.scoreReward}</span></span>
      </div>

      {/* Question */}
      <div className="panel" style={{ marginBottom: 16 }}>
        <div className="panel-label">INTELLIGENCE CHECK</div>
        <div style={{ fontSize: 16, lineHeight: 1.7, minHeight: 60 }}>
          {typedQuestion}
          {typedQuestion.length < (quest?.question?.length || 0) && <span className="blink">_</span>}
        </div>
      </div>

      {/* Choices */}
      <div style={{ display: 'grid', gap: 10, marginBottom: 16 }}>
        {quest?.choices?.map((choice, idx) => {
          const letters = ['A','B','C','D'];
          const isSel   = selected === idx;
          return (
            <button key={idx} className="btn" onClick={() => !submitting && setSelected(idx)}
              style={{
                textAlign: 'left', padding: '12px 16px', fontSize: 13, letterSpacing: 1,
                display: 'flex', gap: 12, alignItems: 'center',
                borderColor: isSel ? 'var(--green)' : 'var(--border)',
                background:  isSel ? 'rgba(57,255,20,0.1)' : 'transparent',
                boxShadow:   isSel ? '0 0 15px rgba(57,255,20,0.2)' : 'none',
              }}>
              <span style={{
                width: 24, height: 24, flexShrink: 0, fontSize: 12,
                border: `1px solid ${isSel ? 'var(--green)' : 'var(--border)'}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: isSel ? 'rgba(57,255,20,0.2)' : 'transparent',
              }}>{letters[idx]}</span>
              {choice}
            </button>
          );
        })}
      </div>

      {/* Actions */}
      <div style={{ display: 'flex', gap: 12 }}>
        <button className="btn btn-amber" onClick={handleSubmit}
          disabled={selected === null || submitting}
          style={{ flex: 1, fontSize: 14, padding: 12, opacity: selected === null ? 0.4 : 1 }}>
          {submitting ? <span>TRANSMITTING<span className="blink">_</span></span> : '► SUBMIT ANSWER'}
        </button>
        <button className="btn btn-dim" onClick={onBack} disabled={submitting}>ABORT</button>
      </div>
    </div>
  );
}

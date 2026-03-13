import React from 'react';

const SKILL_ICONS = {
  Science: '⚗',
  History: '📜',
  Technology: '💻',
  Philosophy: '🧠',
  Mathematics: '∑',
  Literature: '📖',
  Geography: '🌍',
  Politics: '⚖',
};

const SKILL_DESCRIPTIONS = {
  Science: 'Physics, Chemistry, Biology, Astronomy',
  History: 'Ancient, Modern, World Events',
  Technology: 'Computing, Engineering, Invention',
  Philosophy: 'Logic, Ethics, Metaphysics',
  Mathematics: 'Algebra, Calculus, Theory',
  Literature: 'Classic, Modern, Linguistics',
  Geography: 'Nations, Terrain, Demographics',
  Politics: 'Systems, Theory, Global Affairs',
};

export default function SkillsScreen({ player, onSelectSkill }) {
  const skills = player.skills || {};

  return (
    <div className="fade-in">
      <div className="panel" style={{ marginBottom: 16 }}>
        <div className="panel-label">VAULT DWELLER RECORD</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 16, textAlign: 'center' }}>
          {[
            { label: 'LEVEL', value: player.level, color: 'var(--green)' },
            { label: 'TOTAL SCORE', value: player.totalScore.toLocaleString(), color: 'var(--amber)' },
            { label: 'QUESTS DONE', value: player.questsCompleted, color: 'var(--green)' },
            { label: 'STREAK', value: `${player.currentStreak}🔥`, color: 'var(--amber)' },
          ].map(stat => (
            <div key={stat.label}>
              <div style={{ fontSize: 10, color: 'var(--dim-green)', letterSpacing: 2, marginBottom: 4 }}>
                {stat.label}
              </div>
              <div style={{ fontSize: 22, fontFamily: "'VT323', monospace", color: stat.color,
                textShadow: `0 0 10px ${stat.color}` }}>
                {stat.value}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ marginBottom: 12, fontSize: 11, color: 'var(--dim-green)', letterSpacing: 2 }}>
        ► SELECT A SKILL TO BEGIN QUEST SEQUENCE:
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 12 }}>
        {Object.entries(skills).map(([skill, level]) => (
          <div
            key={skill}
            className="panel"
            style={{ cursor: 'pointer', transition: 'all 0.15s' }}
            onClick={() => onSelectSkill(skill)}
            onMouseEnter={e => {
              e.currentTarget.style.borderColor = 'var(--green)';
              e.currentTarget.style.boxShadow = '0 0 15px rgba(57,255,20,0.2)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.borderColor = 'var(--border)';
              e.currentTarget.style.boxShadow = '';
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
              <div>
                <span style={{ fontSize: 18, marginRight: 8 }}>{SKILL_ICONS[skill]}</span>
                <span style={{ fontSize: 14, letterSpacing: 2 }}>{skill.toUpperCase()}</span>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontSize: 10, color: 'var(--dim-green)' }}>LVL </span>
                <span style={{ fontFamily: "'VT323', monospace", fontSize: 22, color: 'var(--amber)',
                  textShadow: '0 0 8px var(--amber)' }}>
                  {level}
                </span>
              </div>
            </div>
            <div className="skill-bar-track" style={{ marginBottom: 8 }}>
              <div className="skill-bar-fill" style={{ width: `${level}%` }} />
            </div>
            <div style={{ fontSize: 10, color: 'var(--dim-green)' }}>
              {SKILL_DESCRIPTIONS[skill]}
            </div>
            <div style={{ marginTop: 8, fontSize: 10, color: 'var(--dim-green)', textAlign: 'right' }}>
              DIFFICULTY: {'▮'.repeat(Math.min(10, Math.floor(level / 10) + 1))}
              {'▯'.repeat(10 - Math.min(10, Math.floor(level / 10) + 1))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

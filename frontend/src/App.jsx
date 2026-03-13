import React, { useState, useEffect } from 'react';
import './styles/global.css';
import Header from './components/Header';
import LoginScreen from './components/LoginScreen';
import SkillsScreen from './components/SkillsScreen';
import QuestScreen from './components/QuestScreen';
import ResultScreen from './components/ResultScreen';
import LeaderboardScreen from './components/LeaderboardScreen';
import { isLoggedIn, getMyPlayer, logout } from './services/api';

const SCREENS = {
  LOGIN:       'login',
  SKILLS:      'skills',
  QUEST:       'quest',
  RESULT:      'result',
  LEADERBOARD: 'leaderboard',
};

export default function App() {
  const [screen,        setScreen]        = useState(SCREENS.LOGIN);
  const [player,        setPlayer]        = useState(null);
  const [selectedSkill, setSelectedSkill] = useState(null);
  const [lastQuest,     setLastQuest]     = useState(null);
  const [lastResult,    setLastResult]    = useState(null);

  // Restore session from stored tokens
  useEffect(() => {
    if (isLoggedIn()) {
      getMyPlayer()
        .then(p => { setPlayer(p); setScreen(SCREENS.SKILLS); })
        .catch(() => {/* tokens expired — stay on login */});
    }
  }, []);

  const handleLogin  = (p)      => { setPlayer(p); setScreen(SCREENS.SKILLS); };

  const handleLogout = async () => {
    await logout();
    setPlayer(null);
    setScreen(SCREENS.LOGIN);
  };

  const handleSelectSkill = (skill) => {
    setSelectedSkill(skill);
    setScreen(SCREENS.QUEST);
  };

  const handleResult = (result, quest) => {
    setPlayer(prev => ({
      ...prev,
      totalScore:       (prev.totalScore || 0) + (result.scoreEarned || 0),
      level:            result.newPlayerLevel,
      questsCompleted:  (prev.questsCompleted || 0) + (result.correct ? 1 : 0),
      currentStreak:    result.correct ? (prev.currentStreak || 0) + 1 : 0,
      skills: { ...prev.skills, [quest.skill]: result.newSkillLevel },
    }));
    setLastQuest(quest);
    setLastResult(result);
    setScreen(SCREENS.RESULT);
  };

  return (
    <div className="app crt-flicker">
      <Header
        player={player}
        screen={screen}
        onLeaderboard={() => setScreen(SCREENS.LEADERBOARD)}
        onLogout={handleLogout}
      />

      {screen === SCREENS.LOGIN && (
        <LoginScreen onLogin={handleLogin} />
      )}

      {screen === SCREENS.SKILLS && player && (
        <SkillsScreen player={player} onSelectSkill={handleSelectSkill} />
      )}

      {screen === SCREENS.QUEST && player && (
        <QuestScreen
          player={player}
          skill={selectedSkill}
          onResult={handleResult}
          onBack={() => setScreen(SCREENS.SKILLS)}
        />
      )}

      {screen === SCREENS.RESULT && lastResult && (
        <ResultScreen
          result={lastResult}
          quest={lastQuest}
          onNext={() => setScreen(SCREENS.QUEST)}
          onBack={() => setScreen(SCREENS.SKILLS)}
          onLeaderboard={() => setScreen(SCREENS.LEADERBOARD)}
        />
      )}

      {screen === SCREENS.LEADERBOARD && (
        <LeaderboardScreen
          currentPlayer={player}
          onBack={() => setScreen(player ? SCREENS.SKILLS : SCREENS.LOGIN)}
        />
      )}

      <div style={{
        textAlign: 'center', fontSize: 10, color: 'var(--dim)',
        letterSpacing: 2, marginTop: 24, paddingTop: 12,
        borderTop: '1px solid var(--border)',
      }}>
        KNOWLEDGE VAULT SYSTEM v2.7.7 — SECURED BY VAULT-TEC SECURITY DIVISION — 2077
      </div>
    </div>
  );
}

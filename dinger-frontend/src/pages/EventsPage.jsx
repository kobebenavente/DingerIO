import { useState, useEffect } from 'react'
import styles from '../styles'
import infoIcon from '../assets/info.png'

const EVENT_PREVIEWS = {
    GAME_DAY_REMINDER: {
        title: 'Game Day Reminder/Overview',
        description: 'Sent 2-3 hours before game starts with a preview of the matchup.',
    },
    GAME_DAY_LINEUP: {
        title: 'Confirmed Starting Lineup',
        description: 'Sent when the official starting lineup is announced before the game.',
    },
    GAME_END: {
        title: 'Game Ended + Final Score',
        description: 'Sent when the game ends with the final score.',
    },
    END_GAME_STANDINGS: {
        title: 'Updated Division Standing + Games Behind',
        description: 'Sent after each game with updated division standings and games behind.',
    },
    BOX_SCORE: {
        title: 'Box Score',
        description: 'Sent after the game ends with the full batting and pitching box score.',
    },
    WEEKLY_SCHEDULE: {
        title: 'Weekly Schedule',
        description: "Sent every Monday with your team's games for the week.",
    },
    END_DAY_STANDINGS: {
        title: 'End-of-Day Division Standings + Games Behind',
        description: 'Sent each evening with the current division standings and games behind.',
    },
    GAME_STARTING: {
        title: 'Game Started',
        description: 'Sent a few minutes before the first inning begins.',
    },
    SCORE_CHANGE: {
        title: 'All Score Changes',
        description: 'Sent whenever any run scores during a live game.',
    },
    LEAD_CHANGE: {
        title: 'Lead Changes Only',
        description: 'Sent only when the lead changes hands during a live game.',
    },
    PITCHER_CHANGE: {
        title: 'Pitcher Changes',
        description: 'Sent whenever a new pitcher enters the game.',
    },
    STARTING_PITCHER_CHANGE: {
        title: 'Starter Pulled Only',
        description: 'Sent only when the starting pitcher is removed from the game.',
    },
    INNING_CHANGE: {
        title: 'End of Inning',
        description: 'Sent at the end of each inning with the current score and pitcher stats.',
    },
    WALKOFF: {
        title: 'Walk-off Situation',
        description: 'Sent when the home team comes to bat in the 9th with a chance to win.',
    },
    EXTRA_INNINGS: {
        title: 'Extra Innings',
        description: 'Sent when the game goes to extra innings.',
    },
    PITCHER_DOMINATING: {
        title: 'Pitcher Dominating',
        description: "Sent when a pitcher is having an exceptional outing (high K's, low hits).",
    },
    NO_HITTER: {
        title: 'No-Hitter In Progress',
        description: 'Sent when a pitcher has a no-hitter going into the late innings.',
    },
}

function Checkbox({ checked }) {
    return (
        <div style={{ position: 'relative', width: '18px', height: '18px', flexShrink: 0 }}>
            <div style={{ width: '18px', height: '18px', borderRadius: '4px', border: '2px solid #000000' }} />
            {checked && (
                <img src="/mark.png" style={{ position: 'absolute', top: '-7px', left: '-3px', width: '28px', height: '28px' }} />
            )}
        </div>
    )
}

function CheckItem({ label, eventKey, checked, onToggle, onInfo }) {
    return (
        <div style={checkItemStyle} onClick={onToggle}>
            <Checkbox checked={checked} />
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <span style={checkLabelStyle}>{label}</span>
                <img
                    src={infoIcon}
                    style={infoIconStyle}
                    onClick={(e) => { e.stopPropagation(); onInfo(eventKey) }}
                />
            </div>
        </div>
    )
}

function MutualCheckRow({ leftKey, leftLabel, rightKey, rightLabel, selected, onToggle, onInfo }) {
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
            <div style={checkItemStyle} onClick={() => onToggle(leftKey, rightKey)}>
                <Checkbox checked={selected.has(leftKey)} />
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <span style={checkLabelStyle}>{leftLabel}</span>
                    <img src={infoIcon} style={infoIconStyle} onClick={(e) => { e.stopPropagation(); onInfo(leftKey) }} />
                </div>
            </div>
            <span style={{ color: '#ffffff', fontWeight: '700', fontSize: '1rem' }}>|</span>
            <div style={checkItemStyle} onClick={() => onToggle(rightKey, leftKey)}>
                <Checkbox checked={selected.has(rightKey)} />
                <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <span style={checkLabelStyle}>{rightLabel}</span>
                    <img src={infoIcon} style={infoIconStyle} onClick={(e) => { e.stopPropagation(); onInfo(rightKey) }} />
                </div>
            </div>
        </div>
    )
}

function SectionCard({ title, titleInfo, onTitleInfo, children }) {
    return (
        <div style={sectionCardStyle}>
            <div style={sectionTitleBannerStyle}>
                <span style={sectionTitleStyle}>{title}</span>
                {titleInfo && (
                    <img
                        src={infoIcon}
                        style={{ ...infoIconStyle, opacity: 0.8 }}
                        onClick={(e) => { e.stopPropagation(); onTitleInfo() }}
                    />
                )}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', padding: '0.75rem 0.25rem 0' }}>
                {children}
            </div>
        </div>
    )
}

function InfoModal({ eventKey, onClose }) {
    const preview = EVENT_PREVIEWS[eventKey]
    if (!preview) return null
    return (
        <div style={modalOverlayStyle} onClick={onClose}>
            <div style={modalCardStyle} onClick={(e) => e.stopPropagation()}>
                <div style={modalHeaderStyle}>
                    <span style={modalTitleStyle}>{preview.title}</span>
                    <span style={modalCloseStyle} onClick={onClose}>✕</span>
                </div>
                <p style={modalDescStyle}>{preview.description}</p>
            </div>
        </div>
    )
}

function EventsPage({ setPage }) {
    const [selected, setSelected] = useState(new Set())
    const [original, setOriginal] = useState(new Set())
    const [mlbTeamId, setMlbTeamId] = useState(null)
    const [saved, setSaved] = useState(false)
    const [error, setError] = useState('')
    const [infoEvent, setInfoEvent] = useState(null)
    const [webhook, setWebhook] = useState('')
    const [originalWebhook, setOriginalWebhook] = useState('')
    const [webhookSaved, setWebhookSaved] = useState(false)
    const [webhookError, setWebhookError] = useState('')

    useEffect(() => {
        const loadSubscription = async () => {
            const token = localStorage.getItem('token')
            const response = await fetch('http://localhost:8080/api/subscription/me', {
                headers: { 'Authorization': `Bearer ${token}` }
            })
            const data = await response.json()
            const events = new Set(data.subbedEvents || [])
            setSelected(events)
            setOriginal(events)
            setMlbTeamId(data.mlbTeamId)
            setWebhook(data.discordWebhookUrl || '')
            setOriginalWebhook(data.discordWebhookUrl || '')
        }
        loadSubscription()
    }, [])

    const toggle = (key) => {
        setSaved(false)
        const next = new Set(selected)
        if (next.has(key)) next.delete(key)
        else next.add(key)
        setSelected(next)
    }

    const toggleExclusive = (key, exclusiveWith) => {
        setSaved(false)
        const next = new Set(selected)
        if (next.has(key)) {
            next.delete(key)
        } else {
            next.add(key)
            next.delete(exclusiveWith)
        }
        setSelected(next)
    }

    const handleSave = async () => {
        setError('')
        const token = localStorage.getItem('token')

        const toAdd = [...selected].filter(e => !original.has(e))
        const toRemove = [...original].filter(e => !selected.has(e))

        if (toAdd.length > 0) {
            const res = await fetch('http://localhost:8080/api/subscription/add-event', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ notificationEvents: toAdd })
            })
            if (!res.ok) { setError('Failed to save changes.'); return }
        }

        if (toRemove.length > 0) {
            const res = await fetch('http://localhost:8080/api/subscription/remove-event', {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ notificationEvents: toRemove })
            })
            if (!res.ok) { setError('Failed to save changes.'); return }
        }

        setOriginal(new Set(selected))
        setSaved(true)
    }

    const handleSaveWebhook = async () => {
        setWebhookError('')
        setWebhookSaved(false)
        const token = localStorage.getItem('token')
        const res = await fetch('http://localhost:8080/api/auth/discord-webhook', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify(webhook)
        })
        if (!res.ok) { setWebhookError('Failed to save.'); return }
        setOriginalWebhook(webhook)
        setWebhookSaved(true)
    }

    const ci = (key, label) => (
        <CheckItem
            key={key}
            eventKey={key}
            label={label}
            checked={selected.has(key)}
            onToggle={() => toggle(key)}
            onInfo={setInfoEvent}
        />
    )

    return (
        <div style={pageStyle}>
            <div style={{ ...styles.logoWrapper, marginBottom: '2rem', marginTop: '-0.5rem' }}>
                <img src="/bell_logo.png" alt="" style={bellStyle} />
                <h1 style={{ ...styles.title, fontSize: '2.6rem', bottom: '10px', textShadow: '0 4px 20px rgba(0,0,0,0.7), 0 2px 6px rgba(0,0,0,0.5)' }}>DingerIO</h1>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.4rem', marginBottom: '1rem' }}>
                <span style={webhookLabelStyle}>Discord Webhook URL</span>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <input
                        style={{ ...styles.input, width: '280px', backgroundColor: '#626262' }}
                        type="text"
                        value={webhook}
                        onChange={(e) => { setWebhook(e.target.value); setWebhookSaved(false) }}
                    />
                    <button style={styles.button} onClick={handleSaveWebhook}>Save</button>
                </div>
                {webhookSaved && <span style={successTextStyle}>Saved!</span>}
                {webhookError && <span style={styles.errorText}>{webhookError}</span>}
            </div>

            <div style={dashboardStyle}>
                <img src="/settings.png" alt="settings" style={settingsIconStyle} onClick={() => setPage('settings')} />

                <div style={panelStyle}>
                    {/* Row 1: Pre Game | Post Game | Schedule & Standings */}
                    <div style={topRowStyle}>
                        <SectionCard title="Pre Game" titleInfo onTitleInfo={() => setInfoEvent('GAME_DAY_REMINDER')}>
                            {ci('GAME_DAY_REMINDER', 'Game Day Reminder/Overview')}
                            {ci('GAME_DAY_LINEUP', 'Confirmed Starting Lineup')}
                        </SectionCard>

                        <SectionCard title="Post Game">
                            {ci('GAME_END', 'Game Ended + Final Score')}
                            {ci('END_GAME_STANDINGS', 'Updated Division Standing + Games Behind')}
                            {ci('BOX_SCORE', 'Box Score')}
                        </SectionCard>

                        <SectionCard title="Schedule & Standings">
                            {ci('WEEKLY_SCHEDULE', 'Weekly Schedule')}
                            {ci('END_DAY_STANDINGS', 'End-of-Day Division Standings + Games Behind')}
                        </SectionCard>
                    </div>

                    {/* Row 2: Live Game | Live Game Tune-In Alerts */}
                    <div style={bottomRowStyle}>
                        <SectionCard title="Live Game">
                            {ci('GAME_STARTING', 'Game Started')}
                            <MutualCheckRow
                                leftKey="SCORE_CHANGE" leftLabel="All Score Changes"
                                rightKey="LEAD_CHANGE" rightLabel="Lead Changes Only"
                                selected={selected}
                                onToggle={toggleExclusive}
                                onInfo={setInfoEvent}
                            />
                            <MutualCheckRow
                                leftKey="PITCHER_CHANGE" leftLabel="Pitcher Changes"
                                rightKey="STARTING_PITCHER_CHANGE" rightLabel="Starter Pulled Only"
                                selected={selected}
                                onToggle={toggleExclusive}
                                onInfo={setInfoEvent}
                            />
                            {ci('INNING_CHANGE', 'End of Inning')}
                        </SectionCard>

                        <SectionCard title='Live Game "Tune-In" Alerts'>
                            {ci('WALKOFF', 'Walk-off situation')}
                            {ci('EXTRA_INNINGS', 'Extra Innings')}
                            {ci('PITCHER_DOMINATING', 'Pitcher Dominating')}
                            {ci('NO_HITTER', 'No-Hitter In Progress')}
                        </SectionCard>
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
                        <button style={styles.button} onClick={handleSave}>Save</button>
                        {saved && <span style={successTextStyle}>Saved!</span>}
                        {error && <span style={styles.errorText}>{error}</span>}
                    </div>
                    {mlbTeamId && (
                        <img src={`/logos/${mlbTeamId}.png`} alt="" style={teamLogoIconStyle} />
                    )}
                </div>
            </div>

            {infoEvent && <InfoModal eventKey={infoEvent} onClose={() => setInfoEvent(null)} />}
        </div>
    )
}

const pageStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    backgroundColor: '#434343',
    fontFamily: "'Quicksand', sans-serif",
    padding: '2rem 1rem',
    boxSizing: 'border-box',
}

const settingsIconStyle = {
    position: 'absolute',
    top: '-48px',
    right: 0,
    width: '40px',
    height: '40px',
    cursor: 'pointer',
    opacity: 0.8,
}

const bellStyle = {
    width: '175px',
    position: 'absolute',
    left: '50%',
    top: '30%',
    transform: 'translateX(-50%) translateY(-50%)',
    zIndex: 0,
    pointerEvents: 'none',
}

const subbedTeamStyle = {
    position: 'absolute',
    top: '-48px',
    left: 0,
    display: 'flex',
    alignItems: 'center',
    gap: '0.4rem',
}

const subbedTeamLabelStyle = {
    color: '#dddddd',
    fontSize: '0.75rem',
    fontWeight: '600',
    fontFamily: "'Quicksand', sans-serif",
    whiteSpace: 'nowrap',
}

const teamLogoIconStyle = {
    position: 'absolute',
    bottom: '1rem',
    right: '1rem',
    width: '80px',
    height: '80px',
    objectFit: 'contain',
    opacity: 0.85,
    pointerEvents: 'none',
}

const dashboardStyle = {
    position: 'relative',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    width: '100%',
    maxWidth: '960px',
}

const panelStyle = {
    position: 'relative',
    backgroundColor: '#545454',
    borderRadius: '12px',
    padding: '1.75rem 1.25rem 1.25rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    width: '100%',
}

const topRowStyle = {
    display: 'flex',
    gap: '1.25rem',
}

const bottomRowStyle = {
    display: 'flex',
    gap: '1.25rem',
    justifyContent: 'center',
}

const sectionCardStyle = {
    flex: 1,
    backgroundColor: '#6c6c6c',
    borderRadius: '10px',
    padding: '0.75rem 1.25rem 1.25rem',
}

const sectionTitleBannerStyle = {
    backgroundColor: '#545454',
    borderRadius: '7px',
    padding: '0.5rem 1.25rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: '0.4rem',
    marginBottom: '0',
}

const sectionTitleStyle = {
    fontSize: '1.2rem',
    fontWeight: '400',
    color: '#ffffff',
    fontFamily: "'Parkinsans', sans-serif",
    textAlign: 'center',
    textShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
}

const checkItemStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: '0.6rem',
    cursor: 'pointer',
}

const checkLabelStyle = {
    color: '#ffffff',
    fontSize: '0.95rem',
    fontWeight: '600',
    fontFamily: "'Quicksand', sans-serif",
}

const successTextStyle = {
    color: '#6ddb6d',
    fontSize: '1rem',
    fontWeight: '600',
    fontFamily: "'Quicksand', sans-serif",
}

const webhookLabelStyle = {
    color: '#dddddd',
    fontSize: '0.8rem',
    fontWeight: '600',
    fontFamily: "'Quicksand', sans-serif",
}

const infoIconStyle = {
    width: '18px',
    height: '18px',
    opacity: 0.6,
    cursor: 'pointer',
    flexShrink: 0,
}

const modalOverlayStyle = {
    position: 'fixed',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    backgroundColor: 'rgba(0, 0, 0, 0.55)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
}

const modalCardStyle = {
    backgroundColor: '#3a3a3a',
    borderRadius: '12px',
    padding: '1.25rem',
    width: '340px',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
}

const modalHeaderStyle = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
}

const modalTitleStyle = {
    fontSize: '1.1rem',
    fontWeight: '700',
    color: '#ffffff',
    fontFamily: "'Parkinsans', sans-serif",
}

const modalCloseStyle = {
    color: '#aaaaaa',
    cursor: 'pointer',
    fontSize: '1rem',
    fontWeight: '600',
}

const modalDescStyle = {
    color: '#cccccc',
    fontSize: '0.85rem',
    fontFamily: "'Quicksand', sans-serif",
    fontWeight: '600',
    margin: 0,
}

export default EventsPage

import { useState, useEffect } from 'react'
import styles from '../styles'

const TOP_SECTIONS = [
    {
        title: 'Reminders',
        events: [
            { key: 'WEEKLY_SCHEDULE', label: 'Weekly Schedule' },
            { key: 'GAME_DAY_REMINDER', label: 'Game Day Reminder' },
            { key: 'GAME_STARTING', label: 'Game Starting' },
        ]
    },
    {
        title: 'Post-Game',
        events: [
            { key: 'END_GAME_STANDINGS', label: 'Division Standings' },
            { key: 'GAME_END', label: 'Game Ended + Final Score' },
        ]
    },
]

const LIVE_EVENTS = [
    { key: 'SCORE_CHANGE', label: 'Score Change' },
    { key: 'HOMERUN', label: 'Home Run' },
    { key: 'HIT', label: 'Hit' },
    { key: 'INNING_CHANGE', label: 'Inning Change' },
    { key: 'HALF_INNING_CHANGE', label: 'Half Inning Change' },
    { key: 'PITCHER_CHANGE', label: 'Pitcher Change' },
    { key: 'STARTING_PITCHER', label: 'Starting Pitcher' },
    { key: 'PROBABLE_PITCHER', label: 'Probable Pitcher' },
    { key: 'END_INNING_PITCHER_STATS', label: 'End Inning Pitcher Stats' },
    { key: 'GAMES_BEHIND_FIRST', label: 'Games Behind First' },
    { key: 'GAME_BEHIND_WILDCARD', label: 'Games Behind Wildcard' },
    { key: 'END_DAY_STANDINGS', label: 'End of Day Standings' },
]

function CheckItem({ label, checked, onToggle }) {
    return (
        <div style={checkItemStyle} onClick={onToggle}>
            <div style={{ position: 'relative', width: '18px', height: '18px', flexShrink: 0 }}>
                <div style={{ width: '18px', height: '18px', borderRadius: '4px', border: '2px solid #000000' }} />
                {checked && (
                    <img src="/mark.png" style={{ position: 'absolute', top: '-7px', left: '-3px', width: '28px', height: '28px' }} />
                )}
            </div>
            <span style={checkLabelStyle}>{label}</span>
        </div>
    )
}

function EventsPage({ setPage }) {
    const [selected, setSelected] = useState(new Set())
    const [original, setOriginal] = useState(new Set())
    const [mlbTeamId, setMlbTeamId] = useState(null)
    const [saved, setSaved] = useState(false)
    const [error, setError] = useState('')

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
        }
        loadSubscription()
    }, [])

    const toggle = (key) => {
        setSaved(false)
        const next = new Set(selected)
        if (next.has(key)) {
            next.delete(key)
        } else {
            next.add(key)
        }
        setSelected(next)
    }

    const handleSave = async () => {
        setError('')
        const token = localStorage.getItem('token')

        const toAdd = []
        for (const event of selected) {
            if (!original.has(event)) {
                toAdd.push(event)
            }
        }

        const toRemove = []
        for (const event of original) {
            if (!selected.has(event)) {
                toRemove.push(event)
            }
        }

        if (toAdd.length > 0) {
            const res = await fetch('http://localhost:8080/api/subscription/add-event', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ notificationEvents: toAdd })
            })
            if (!res.ok) {
                setError('Failed to save changes.')
                return
            }
        }

        if (toRemove.length > 0) {
            const res = await fetch('http://localhost:8080/api/subscription/remove-event', {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                body: JSON.stringify({ notificationEvents: toRemove })
            })
            if (!res.ok) {
                setError('Failed to save changes.')
                return
            }
        }

        setOriginal(new Set(selected))
        setSaved(true)
    }

return (
    <div style={pageStyle}>
        <div style={{ ...styles.logoWrapper, marginBottom: '1.5rem' }}>
            {mlbTeamId && (
                <img src={`/logos/${mlbTeamId}.png`} alt="" style={teamLogoStyle} />
            )}
            <h1 style={{ ...styles.title, bottom: '10px', textShadow: '0 4px 20px rgba(0,0,0,0.7), 0 2px 6px rgba(0,0,0,0.5)' }}>DingerIO</h1>
        </div>
        <div style={dashboardStyle}>
            <img src="/settings.png" alt="settings" style={settingsIconStyle} onClick={() => setPage('settings')} />
            <div style={panelStyle}>
                <div style={topRowStyle}>
                    {TOP_SECTIONS.map(section => (
                        <div key={section.title} style={{ ...sectionCardStyle, flex: 1 }}>
                            <p style={sectionTitleStyle}>{section.title}</p>
                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                                {section.events.map(event => (
                                    <CheckItem
                                        key={event.key}
                                        label={event.label}
                                        checked={selected.has(event.key)}
                                        onToggle={() => toggle(event.key)}
                                    />
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
                <div style={sectionCardStyle}>
                    <p style={sectionTitleStyle}>Live Game Updates</p>
                    <div style={liveGridStyle}>
                        {LIVE_EVENTS.map(event => (
                            <CheckItem
                                key={event.key}
                                label={event.label}
                                checked={selected.has(event.key)}
                                onToggle={() => toggle(event.key)}
                            />
                        ))}
                    </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
                    <button style={styles.button} onClick={handleSave}>Save</button>
                    {saved && <span style={successTextStyle}>Saved!</span>}
                    {error && <span style={styles.errorText}>{error}</span>}
                </div>
            </div>
        </div>
    </div>
)
}

const pageStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    backgroundColor: '#737373',
    fontFamily: "'Quicksand', sans-serif",
    padding: '2rem 1rem',
    boxSizing: 'border-box',
}

const contentWrapperStyle = {
    position: 'relative',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '1.5rem',
    width: '100%',
    maxWidth: '700px',
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

const teamLogoStyle = {
    position: 'absolute',
    width: '135px',
    height: '135px',
    objectFit: 'contain',
    opacity: 0.7,
    pointerEvents: 'none',
    bottom: '-10px'
}

const dashboardStyle = {
    position: 'relative',
    display: 'flex',
    flexDirection: 'column',
    gap: '1.25rem',
    width: '100%',
    maxWidth: '850px',
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

const sectionCardStyle = {
    backgroundColor: '#868686',
    borderRadius: '10px',
    padding: '1.25rem 1.5rem',
}

const sectionTitleStyle = {
    fontSize: '1.4rem',
    fontWeight: '700',
    color: '#ffffff',
    fontFamily: "'Parkinsans', sans-serif",
    marginBottom: '0.9rem',
    textAlign: 'center',
    textShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
}

const liveGridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gap: '0.75rem 1.25rem',
}

const checkItemStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: '0.6rem',
    cursor: 'pointer',
}

const checkLabelStyle = {
    color: '#ffffff',
    fontSize: '0.98rem',
    fontWeight: '600',
    fontFamily: "'Quicksand', sans-serif",
}

const successTextStyle = {
    color: '#6ddb6d',
    fontSize: '1rem',
    fontWeight: '600',
    fontFamily: "'Quicksand', sans-serif",
}

export default EventsPage

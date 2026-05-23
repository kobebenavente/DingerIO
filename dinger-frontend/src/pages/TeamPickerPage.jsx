import { useState, useEffect } from 'react'
import styles from '../styles'

function TeamPickerPage({ setPage, isChange }) {
    const [teams, setTeams] = useState([])
    const [selected, setSelected] = useState(null)
    const [error, setError] = useState('')

    useEffect(() => {
        const loadTeams = async () => {
            const token = localStorage.getItem('token')
            const response = await fetch('http://localhost:8080/api/teams/all', {
                headers: { 'Authorization': `Bearer ${token}` }
            })
            const data = await response.json()
            setTeams(data)
        }
        loadTeams()
    }, [])

    const handleConfirm = async () => {
        if (!selected) return

        const token = localStorage.getItem('token')

        let url = 'http://localhost:8080/api/subscription/create'
        let method = 'POST'

        if (isChange) {
            url = 'http://localhost:8080/api/subscription/change'
            method = 'PATCH'
        }

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ teamId: selected })
        })

        if (response.ok) {
            setPage('events')
        } else {
            setError('Something went wrong. Please try again.')
        }
    }

    const getCardStyle = (teamId) => {
        if (selected === teamId) {
            return { ...teamCardStyle, outline: '2px solid #ffffff' }
        }
        return { ...teamCardStyle, outline: '2px solid transparent' }
    }

    return (
        <div style={styles.container}>
            <h1 style={{ ...styles.title, marginBottom: '0.5rem' }}>Pick Your Team</h1>
            <p style={{ ...styles.subtitle, marginBottom: '1.5rem' }}>Choose the MLB team you want notifications for</p>
            <div style={gridStyle}>
                {teams.map(team => (
                    <div
                        key={team.mlbTeamId}
                        onClick={() => setSelected(team.teamId)}
                        style={getCardStyle(team.teamId)}
                    >
                        <img src={`/logos/${team.mlbTeamId}.png`} alt={team.teamName} style={logoStyle} />
                        <span style={teamNameStyle}>{team.teamName}</span>
                    </div>
                ))}
            </div>
            {error && <p style={{ ...styles.errorText, marginTop: '1rem' }}>{error}</p>}
            <button
                style={{ ...styles.button, marginTop: '1.5rem', opacity: selected ? 1 : 0.4 }}
                onClick={handleConfirm}
            >
                Confirm
            </button>
            {isChange && (
                <p style={{ ...styles.linkText, marginTop: '0.75rem' }} onClick={() => setPage('settings')}>Back</p>
            )}
        </div>
    )
}

const gridStyle = {
    display: 'grid',
    gridTemplateColumns: 'repeat(6, 1fr)',
    gap: '0.75rem',
    maxWidth: '780px',
    width: '100%',
    padding: '0 1rem',
}

const teamCardStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '0.4rem',
    padding: '0.6rem',
    borderRadius: '10px',
    backgroundColor: '#626262',
    cursor: 'pointer',
}

const logoStyle = {
    width: '52px',
    height: '52px',
    objectFit: 'contain',
}

const teamNameStyle = {
    fontSize: '0.6rem',
    color: '#ffffff',
    textAlign: 'center',
    fontFamily: "'Quicksand', sans-serif",
    fontWeight: '600',
}

export default TeamPickerPage

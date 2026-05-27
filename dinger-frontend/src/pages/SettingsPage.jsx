import styles from '../styles'

function SettingsPage({ setPage, setIsLoggedIn }) {

    const handleLogout = () => {
        localStorage.removeItem('token')
        setIsLoggedIn(false)
        setPage('home')
    }

    return (
        <div style={styles.container}>
            <div style={styles.logoWrapper}>
                <h1 style={styles.title}>Settings</h1>
            </div>
            <div style={cardGroupStyle}>

                <div style={settingsCardStyle}>
                    <div style={rowStyle} onClick={() => setPage('team-picker-change')}>
                        <span style={rowLabelStyle}>Change Team</span>
                        <span style={chevronStyle}>›</span>
                    </div>
                </div>

                <div style={{ ...settingsCardStyle, cursor: 'pointer' }} onClick={handleLogout}>
                    <div style={rowStyle}>
                        <span style={logoutLabelStyle}>Log Out</span>
                    </div>
                </div>

            </div>
            <p style={backLinkStyle} onClick={() => setPage('events')}>Back</p>
        </div>
    )
}

const cardGroupStyle = {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    width: '320px',
}

const settingsCardStyle = {
    backgroundColor: '#626262',
    borderRadius: '10px',
    padding: '0.9rem 1.25rem',
}

const rowStyle = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    cursor: 'pointer',
}

const rowLabelStyle = {
    fontSize: '0.9rem',
    fontWeight: '600',
    color: '#ffffff',
    fontFamily: "'Quicksand', sans-serif",
    textShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
}

const logoutLabelStyle = {
    fontSize: '0.9rem',
    fontWeight: '600',
    color: '#ff6b6b',
    fontFamily: "'Quicksand', sans-serif",
}

const chevronStyle = {
    color: '#ffffff',
    fontSize: '0.85rem',
    textShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
}

const backLinkStyle = {
    color: '#ffffff',
    fontSize: '1rem',
    fontWeight: '700',
    textAlign: 'center',
    cursor: 'pointer',
    marginTop: '1.5rem',
    textShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
    fontFamily: "'Quicksand', sans-serif",
}

export default SettingsPage

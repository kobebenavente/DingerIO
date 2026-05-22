import { useState } from 'react'
import './App.css'
import swingImg from './assets/swing.png'

const styles = {
    container: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100vh',
        backgroundColor: '#737373',
        fontFamily: "'Quicksand', sans-serif",
    },
    logoWrapper: {
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    swingImage: {
        position: 'absolute',
        width: '110px',
        opacity: 0.6,
        pointerEvents: 'none',
        top: '-60px',
    },
    title: {
        fontSize: '3rem',
        fontWeight: '400',
        color: '#ffffff',
        fontFamily: "'Parkinsans', sans-serif",
        margin: '0',
        lineHeight: '1.4',
        textShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
        position: 'relative',
    },
    subtitle: {
        fontSize: '0.95rem',
        fontWeight: '600',
        color: '#ffffff',
        marginTop: '0.75rem',
        marginBottom: '1.75rem',
        textShadow: '0 2px 12px rgba(0, 0, 0, 0.4)',
    },
    buttonRow: {
        display: 'flex',
        gap: '0.75rem',
    },
    button: {
        padding: '0.5rem 1.5rem',
        fontSize: '0.85rem',
        fontFamily: "'Quicksand', sans-serif",
        fontWeight: '600',
        borderRadius: '8px',
        border: 'none',
        cursor: 'pointer',
        backgroundColor: '#ffffff',
        color: '#0f0f0f',
    },
}

function App() {
    const [page, setPage] = useState('home')

    if (page === 'home') {
        return (
            <div style={styles.container}>
                <div style={styles.logoWrapper}>
                    <img src={swingImg} alt="" style={styles.swingImage} />
                    <h1 style={styles.title}>DingerIO</h1>
                </div>
                <p style={styles.subtitle}>A customizable Discord notification service for your favorite MLB team</p>
                <div style={styles.buttonRow}>
                    <button style={styles.button} onClick={() => setPage('login')}>Log In</button>
                    <button style={styles.button} onClick={() => setPage('signup')}>Sign Up</button>
                    <button style={styles.button} onClick={() => setPage('about')}>About</button>
                </div>
            </div>
        )
    }

    return (
        <div style={styles.container}>
            <p style={{ color: '#ffffff' }}>{page} page coming soon</p>
            <button style={styles.button} onClick={() => setPage('home')}>Back</button>
        </div>
    )
}

export default App

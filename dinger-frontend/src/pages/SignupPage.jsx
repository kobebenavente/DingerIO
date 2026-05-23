import { useState } from 'react'
import styles from '../styles'

function SignupPage({ setPage }) {
    const [email, setEmail] = useState('')
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')

    const handleSignup = async () => {
        setError('')
        const response = await fetch('http://localhost:8080/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, username, password })
        })

        if (response.ok) {
            setPage('login')
        } else {
            const message = await response.text()
            setError(message)
        }
    }

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <p style={styles.cardTitle}>Sign Up</p>
                <input style={styles.input} type="email" placeholder="Email" onChange={e => setEmail(e.target.value)} />
                <input style={styles.input} type="text" placeholder="Username" onChange={e => setUsername(e.target.value)} />
                <input style={styles.input} type="password" placeholder="Password" onChange={e => setPassword(e.target.value)} />
                {error && <p style={styles.errorText}>{error}</p>}
                <button style={styles.button} onClick={handleSignup}>Sign Up</button>
                <p style={styles.linkText} onClick={() => setPage('login')}>Already have an account? Log in</p>
                <p style={styles.linkText} onClick={() => setPage('home')}>Back</p>
            </div>
        </div>
    )
}

export default SignupPage

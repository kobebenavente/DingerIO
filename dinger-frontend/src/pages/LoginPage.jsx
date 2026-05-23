import { useState } from 'react'
import styles from '../styles'

function LoginPage({ setPage, onLoginSuccess }) {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')

    const handleLogin = async () => {
        setError('')
        const response = await fetch('http://localhost:8080/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        })

        if (response.ok) {
            const token = await response.text()
            localStorage.setItem('token', token)
            onLoginSuccess()
        } else {
            const message = await response.text()
            setError(message)
        }
    }

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <p style={styles.cardTitle}>Log In</p>
                <input style={styles.input} type="email" placeholder="Email" onChange={e => setEmail(e.target.value)} />
                <input style={styles.input} type="password" placeholder="Password" onChange={e => setPassword(e.target.value)} />
                {error && <p style={styles.errorText}>{error}</p>}
                <button style={styles.button} onClick={handleLogin}>Log In</button>
                <p style={styles.linkText} onClick={() => setPage('signup')}>Don't have an account? Sign up</p>
                <p style={styles.linkText} onClick={() => setPage('home')}>Back</p>
            </div>
        </div>
    )
}

export default LoginPage

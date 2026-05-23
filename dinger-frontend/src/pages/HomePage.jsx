import styles from '../styles'

function HomePage({ setPage, isLoggedIn, setIsLoggedIn }) {
    return (
        <div style={styles.container}>
            <div style={styles.logoWrapper}>
                <img src="/logo.png" alt="" style={styles.swingImage} />
                <h1 style={styles.title}>DingerIO</h1>
            </div>
            <p style={styles.subtitle}>A customizable Discord webhook bot made for keeping up with your MLB team of choice</p>
            <div style={styles.buttonRow}>
                {isLoggedIn
                    ? <button style={styles.button} onClick={() => { localStorage.removeItem('token'); setIsLoggedIn(false) }}>Log Out</button>
                    : <>
                        <button style={styles.button} onClick={() => setPage('login')}>Log In</button>
                        <button style={styles.button} onClick={() => setPage('signup')}>Sign Up</button>
                      </>
                }
                <button style={styles.button} onClick={() => setPage('about')}>About</button>
            </div>
        </div>
    )
}

export default HomePage

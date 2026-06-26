import styles from '../styles'

function HomePage({ setPage, isLoggedIn, setIsLoggedIn }) {
    return (
        <div style={{ ...styles.container, position: 'relative' }}>
            <div style={styles.logoWrapper}>
                <img src="/bell_logo.png" alt="" style={styles.swingImage} />
                <h1 style={styles.title}>DingerIO</h1>
            </div>
            <div style={styles.buttonRow}>
                {isLoggedIn
                    ? <span style={styles.navLink} onClick={() => { localStorage.removeItem('token'); setIsLoggedIn(false) }}>log out</span>
                    : <>
                        <span style={styles.navLink} onClick={() => setPage('login')}>log in</span>
                        <span style={styles.navLink} onClick={() => setPage('signup')}>sign up</span>
                        <span style={styles.navLink}>about</span>
                      </>
                }
            </div>
            <div style={styles.scrollIndicator}>
                <div style={styles.scrollChevron} />
            </div>
        </div>
    )
}

export default HomePage

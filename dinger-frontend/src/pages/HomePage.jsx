import swingImg from '../assets/swing.png'
import styles from '../styles'

function HomePage({ setPage, isLoggedIn, setIsLoggedIn }) {
    return (
        <div style={styles.container}>
            <div style={styles.logoWrapper}>
                <img src={swingImg} alt="" style={styles.swingImage} />
                <h1 style={styles.title}>DingerIO</h1>
            </div>
            <p style={styles.subtitle}>A customizable Discord notification service for your MLB team of choice</p>
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

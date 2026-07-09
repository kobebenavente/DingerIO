import styles from '../styles'

function HomePage({ setPage, isLoggedIn, setIsLoggedIn }) {
    return (
        <div style={containerStyle}>
            <div style={leftColumnStyle}>
                <div style={logoWrapperStyle}>
                    <img src="/bell_logo.png" alt="" style={bellStyle} />
                    <h1 style={titleStyle}>DingerIO</h1>
                </div>
                <p style={captionStyle}>
                    Get live, configurable updates for your favorite Major League Baseball team sent directly to your Discord server for free!
                </p>
                <div style={navStyle}>
                    {isLoggedIn
                        ? <span style={navLinkStyle} onClick={() => { localStorage.removeItem('token'); setIsLoggedIn(false) }}>log out</span>
                        : <>
                            <span style={navLinkStyle} onClick={() => setPage('login')}>log in</span>
                            <span style={navLinkStyle} onClick={() => setPage('signup')}>sign up</span>
                            <span style={navLinkStyle}>learn more</span>
                          </>
                    }
                </div>
            </div>
            <div style={rightColumnStyle} />
        </div>
    )
}

const containerStyle = {
    display: 'flex',
    flexDirection: 'row',
    height: '100vh',
    backgroundColor: '#4a4a4a',
    fontFamily: "'Zalando Sans Expanded', sans-serif",
}

const leftColumnStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    width: '50%',
    padding: '0 3rem',
    gap: '2.5rem',
}

const rightColumnStyle = {
    width: '50%',
}

const logoWrapperStyle = {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
}

const bellStyle = {
    width: '220px',
    position: 'absolute',
    left: '50%',
    top: '50%',
    transform: 'translateX(-50%) translateY(-50%)',
    zIndex: 0,
    pointerEvents: 'none',
}

const titleStyle = {
    fontSize: '3.5rem',
    fontWeight: '300',
    color: '#ffffff',
    fontFamily: "'Zalando Sans Expanded', sans-serif",
    margin: 0,
    position: 'relative',
    zIndex: 1,
    transform: 'translateY(12px)',
    textShadow: '0 0 30px rgba(255,255,255,0.2), 0 2px 16px rgba(0,0,0,0.6)',
}

const captionStyle = {
    fontSize: '1.15rem',
    fontWeight: '400',
    color: '#ffffff',
    lineHeight: '1.6',
    margin: 0,
    marginTop: '2rem',
    maxWidth: '480px',
    textAlign: 'center',
}

const navStyle = {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '1.25rem',
}

const navLinkStyle = {
    color: '#c1bba0',
    fontSize: '1.8rem',
    fontFamily: "'Zalando Sans Expanded', sans-serif",
    fontWeight: '400',
    cursor: 'pointer',
    letterSpacing: '0.02em',
    textShadow: '0 0 16px rgba(60,57,45,1)',
    userSelect: 'none',
}

export default HomePage

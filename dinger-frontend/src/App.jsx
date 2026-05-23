import { useState, useEffect } from 'react'
import './App.css'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import TeamPickerPage from './pages/TeamPickerPage'
import EventsPage from './pages/EventsPage'
import SettingsPage from './pages/SettingsPage'

function App() {
    const [page, setPage] = useState('home')
    const [isLoggedIn, setIsLoggedIn] = useState(localStorage.getItem('token') !== null)

    const routeBySubInfo = async (token) => {
        const response = await fetch('http://localhost:8080/api/subscription/me', {
            headers: { 'Authorization': `Bearer ${token}` }
        })

        if (!response.ok) {
            setPage('home')
            return
        }

        const sub = await response.json()

        if (!sub.teamId) {
            setPage('team-picker')
        } else {
            setPage('events')
        }
    }

    useEffect(() => {
        const token = localStorage.getItem('token')
        if (token) {
            routeBySubInfo(token)
        }
    }, [])

    const handleLoginSuccess = () => {
        setIsLoggedIn(true)
        routeBySubInfo(localStorage.getItem('token'))
    }

    if (page === 'login') return <LoginPage setPage={setPage} onLoginSuccess={handleLoginSuccess} />
    if (page === 'signup') return <SignupPage setPage={setPage} />
    if (page === 'team-picker') return <TeamPickerPage setPage={setPage} />
    if (page === 'team-picker-change') return <TeamPickerPage setPage={setPage} isChange={true} />
    if (page === 'events') return <EventsPage setPage={setPage} />
    if (page === 'settings') return <SettingsPage setPage={setPage} setIsLoggedIn={setIsLoggedIn} />
    return <HomePage setPage={setPage} isLoggedIn={isLoggedIn} setIsLoggedIn={setIsLoggedIn} />
}

export default App

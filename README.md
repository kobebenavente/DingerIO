<p align="center">
  <img src="dinger-frontend/public/bell_logo.png" alt="DingerIO Logo" width="150"><br>
  <b>Get notified on your favorite Major League Baseball team via Discord — scores, lineups, weekly schedules, standings updates, and more.</b>
  <br>
  <br>
</p>

## What is DingerIO?
DingerIO is a free, Major League Baseball notification service that sends team-specific updates directly to your Discord server via webhooks. The application runs entirely on your own machine, but online deployment is in progress. 

Users can create an account, select a team of their choice, then configure up to 17 different notification types through a dashboard, allowing for updates that are tailored to your needs and can be changed at any time.

![Dashboard Screenshot](docs/dashboard2.png)

![Scoring_Notification_Screenshot](docs/scoring_screenshot.png)

![Standings_Screenshot](docs/standings_message.png)


---

## How It Works
DingerIO works by connecting to the official MLB Stats API, which provides live game data, team information, and full player rosters for every team in the league. When the application first starts up, it syncs and stores all 30 MLB teams and their rosters into a local database, so subscription lookups are fast and don't require repeated calls to the external API.

From there, the backend runs two scheduled processes. The first runs every Monday morning and fetches the upcoming 7-day schedule for each subscribed team, sending users a preview of their team's games for the week. The second is the main polling loop, which runs every 15 seconds throughout the day. On each cycle, it fetches the schedule for the current date to get a list of all games being played that day. For each game, it checks which users are subscribed to either team and routes the game through one of three handlers depending on its status.

If a game hasn't started yet, the pre-game handler checks how far away the first pitch is. A game day reminder is sent a few hours before the game, and a second notification is sent within minutes of the first pitch. Once a game is live, the live game handler fetches the full real-time game feed and compares it against the last recorded state. If anything has changed — a run scored, a new pitcher entered, an inning ended — it sends the appropriate notification to every subscribed user who has that event turned on. When a game ends, the final score is sent along with updated division standings if the user has that event enabled. If a game is postponed, a notification is sent letting users know it will not be played that day.

To prevent duplicate notifications, each game tracks a state object that records the current score, inning, and active pitchers. A notification is only sent when a change is detected from the previously recorded state

## Tech Stack
- **Java / Spring Boot** — backend application and scheduling
- **PostgreSQL** — persistent storage for users, teams, players, and subscriptions
- **MLB Stats API** — source for live game data and roster information
- **Discord Webhooks** — delivery channel for real-time notifications
- **React** — frontend dashboard for managing notification preferences
- **Vite** — frontend build tool and development server
- **JWT (JSON Web Tokens)** — stateless user authentication


---

## Status
Majority of planned features have been implemented and are operating as intended. Next step are adding the remaining notification events and deploying the application and database to a cloud server so that the application does not need to be run locally.

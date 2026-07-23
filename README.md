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

## Running DingerIO On Your Machine

### Prerequisites
- Java 21
- Node.js
- Docker (for the PostgreSQL container)                                                                                                                                                     
- Maven (included via `mvnw` wrapper — no install needed)

### 1. Clone the repository
```bash
git clone https://github.com/kobebenavente/DingerIO.git
cd DingerIO
```

### 2. Configure environment variables
```bash
cp .env.example .env
```
Open `.env` and fill in your values. The database credentials must match what Docker uses to start the PostgreSQL container.

### 3. Start the database
```bash
docker-compose up -d
```

### 4. Start the backend
```bash
./mvnw spring-boot:run
```

### 5. Start the frontend
```bash
cd dinger-frontend
npm install
npm run dev
```

The app will be available at http://localhost:5173

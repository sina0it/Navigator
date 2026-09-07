import express, { Request, Response } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';

dotenv.config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

// Request logging middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// System Health Check
app.get('/api/health', (req: Request, res: Response) => {
  res.json({
    status: 'healthy',
    application: 'Sina Navigator Backend API',
    developer: 'Sina Naderi',
    developerEmail: 'sinananderi203@gmail.com',
    timestamp: new Date().toISOString(),
    services: {
      database: 'connected',
      routingEngine: 'OSRM online',
      geocoding: 'OpenStreetMap Nominatim',
      aiAssistant: 'Sina AI Gemini Engine'
    }
  });
});

// Mock in-memory store for standalone execution if PostgreSQL isn't attached
let users = [
  {
    id: 1,
    username: 'sinanaderi',
    email: 'sinananderi203@gmail.com',
    fullName: 'Sina Naderi',
    role: 'SUPER_ADMIN',
    isActive: true,
    preferredLanguage: 'en',
    voiceGuidance: true,
    avoidTolls: false,
    avoidHighways: false
  },
  {
    id: 2,
    username: 'demo_user',
    email: 'user@example.com',
    fullName: 'Demo Navigator User',
    role: 'USER',
    isActive: true,
    preferredLanguage: 'en',
    voiceGuidance: true,
    avoidTolls: false,
    avoidHighways: false
  }
];

let favorites = [
  { id: 1, userId: 1, name: 'Home Sweet Home', address: '104 Boulevard Central', category: 'HOME', latitude: 37.7749, longitude: -122.4194, notes: 'Main residence' },
  { id: 2, userId: 1, name: 'Tech HQ', address: '500 Innovation Way', category: 'WORK', latitude: 37.7833, longitude: -122.4167, notes: 'Engineering building' }
];

let savedTrips = [
  {
    id: 1,
    userId: 1,
    title: 'Weekend Coast Drive',
    description: 'Scenic highway itinerary with cliff views',
    travelMode: 'DRIVING',
    totalDistanceKm: 42.5,
    totalDurationMin: 55,
    stops: [
      { name: 'Bay Point Lookout', lat: 37.77, lng: -122.42 },
      { name: 'Seaside Cafe', lat: 37.78, lng: -122.40 }
    ]
  }
];

let notifications = [
  { id: 1, userId: 1, title: 'Welcome to Sina Navigator', message: 'Engine and GPS ready. Have a safe journey!', type: 'INFO', isRead: false, createdAt: new Date() },
  { id: 2, userId: 1, title: 'Traffic Advisory', message: 'Moderate delay reported on Route 101 due to construction.', type: 'TRAFFIC', isRead: false, createdAt: new Date() }
];

// --- AUTH ROUTES ---
app.post('/api/auth/login', (req: Request, res: Response) => {
  const { email, password } = req.body;
  const user = users.find(u => u.email.toLowerCase() === (email || '').toLowerCase());
  if (!user) {
    return res.status(401).json({ error: 'Invalid email or password' });
  }
  return res.json({
    token: `sina_nav_token_${user.id}_${Date.now()}`,
    user
  });
});

app.post('/api/auth/register', (req: Request, res: Response) => {
  const { username, email, fullName } = req.body;
  const existing = users.find(u => u.email.toLowerCase() === (email || '').toLowerCase());
  if (existing) {
    return res.status(400).json({ error: 'Email already registered' });
  }
  const newUser = {
    id: users.length + 1,
    username,
    email,
    fullName,
    role: 'USER',
    isActive: true,
    preferredLanguage: 'en',
    voiceGuidance: true,
    avoidTolls: false,
    avoidHighways: false
  };
  users.push(newUser);
  return res.status(201).json({
    token: `sina_nav_token_${newUser.id}_${Date.now()}`,
    user: newUser
  });
});

// --- FAVORITES ROUTES ---
app.get('/api/favorites', (req: Request, res: Response) => {
  return res.json(favorites);
});

app.post('/api/favorites', (req: Request, res: Response) => {
  const newFav = { id: favorites.length + 1, userId: 1, ...req.body };
  favorites.push(newFav);
  return res.status(201).json(newFav);
});

app.delete('/api/favorites/:id', (req: Request, res: Response) => {
  const id = parseInt(req.params.id);
  favorites = favorites.filter(f => f.id !== id);
  return res.json({ success: true });
});

// --- TRIPS ROUTES ---
app.get('/api/trips', (req: Request, res: Response) => {
  return res.json(savedTrips);
});

app.post('/api/trips', (req: Request, res: Response) => {
  const newTrip = { id: savedTrips.length + 1, userId: 1, ...req.body };
  savedTrips.push(newTrip);
  return res.status(201).json(newTrip);
});

// --- NOTIFICATIONS ROUTES ---
app.get('/api/notifications', (req: Request, res: Response) => {
  return res.json(notifications);
});

app.post('/api/notifications/read-all', (req: Request, res: Response) => {
  notifications = notifications.map(n => ({ ...n, isRead: true }));
  return res.json({ success: true });
});

// --- ADMIN STATS ROUTES ---
app.get('/api/admin/stats', (req: Request, res: Response) => {
  return res.json({
    totalUsers: users.length,
    activeUsers: users.filter(u => u.isActive).length,
    totalRoutesCalculated: 148,
    totalSavedPlaces: favorites.length,
    totalTripsPlanned: savedTrips.length,
    aiAssistantInteractions: 89,
    systemStatus: 'ONLINE_HEALTHY'
  });
});

app.post('/api/admin/broadcast', (req: Request, res: Response) => {
  const { title, message } = req.body;
  const newNotif = {
    id: notifications.length + 1,
    userId: 1,
    title,
    message,
    type: 'BROADCAST',
    isRead: false,
    createdAt: new Date()
  };
  notifications.unshift(newNotif);
  return res.json({ success: true, notification: newNotif });
});

// Fallback error handler
app.use((err: any, req: Request, res: Response, next: any) => {
  console.error('API Error:', err);
  res.status(500).json({ error: 'Internal Server Error', message: err.message });
});

app.listen(PORT, () => {
  console.log(`=========================================`);
  console.log(` Sina Navigator REST API is running`);
  console.log(` Port: ${PORT}`);
  console.log(` Developer: Sina Naderi (sinananderi203@gmail.com)`);
  console.log(`=========================================`);
});

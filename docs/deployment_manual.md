# Deployment Manual (Render + Vercel)

I have optimized the project for a split deployment: **Render** for the backend and **Vercel** for the frontend.

## 1. Backend Deployment (Render)

### Steps
1. **GitHub/GitLab**: Push your `digital-memory-engine` folder to a repository.
2. **Render Dashboard**:
   - Click **"New +"** and select **"Blueprint"**.
   - Connect your repository.
   - Render will detect `render.yaml` and create the `digital-memory-engine` service.
3. **Environment Variables**:
   - Fill in the values from your [secrets.env](file:///Users/vinayakg08/.gemini/antigravity/brain/f3dd8f00-b0f5-4f68-86ed-3497e170a5c8/secrets.env) when prompted.

---

## 2. Frontend Deployment (Vercel)

### Steps
1. **GitHub**: Push your `digital-memory-ui` folder to a repository.
2. **Vercel Dashboard**:
   - Click **"Add New"** -> **"Project"**.
   - Select your repository.
   - Set the **Root Directory** to `digital-memory-ui`.
3. **Environment Variables**:
   - Add `VITE_API_BASE_URL`.
   - **Value**: `https://digital-memory-engine.onrender.com/api/v1`.
4. **Deploy**: Vercel will build and host your UI with global CDN performance.

---

## 3. Keep-Alive (For Render Free Tier)
To prevent your backend from sleeping (spinning down after 15 mins of inactivity):

### Option A: UptimeRobot (Recommended)
1. Go to [uptimerobot.com](https://uptimerobot.com/) and create a free account.
2. Click **"Add New Monitor"**.
3. **Monitor Type**: Select **"HTTP(s)"**.
4. **Friendly Name**: `Digital Memory Backend`
5. **URL**: `https://digital-memory-engine.onrender.com/actuator/health`
6. **Monitoring Interval**: Set to **5 minutes** (Critical).
7. Click **Create Monitor**.

### Option B: Frontend Hook (Fallback)
The [useKeepAlive.ts](file:///Users/vinayakg08/Documents/digital-memory-ui/src/hooks/useKeepAlive.ts) hook is integrated into `App.tsx` and will ping the server every 5 minutes **only while the tab is open**.

## 4. Summary of URLs
- **Backend**: `https://digital-memory-engine.onrender.com`
- **Frontend**: `https://your-project.vercel.app`

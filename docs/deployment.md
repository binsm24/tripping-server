# Backend deployment

## Validation before merge

Use Java 21 and run `./gradlew clean test bootJar` (Windows: `gradlew.bat clean test bootJar`).
The application tests replace Firebase with mocks and use a test-only JWT signing key.
They check startup, public `/health`, protected saved courses, and allowed/disallowed CORS origins.
These tests do not validate production API credentials or map rendering on Vercel.

Commit the deployment changes to `feature/groq-api`, push them, then merge that branch into `main`.
Importing the old `main` without those commits will not deploy the current backend.

## Vercel

The repository root contains `Dockerfile.vercel`, which builds/tests the Java 21 application
and runs it with Playwright 1.58.0 browsers. Keep the image version aligned with build.gradle.
Vercel Container Images is a beta feature: https://vercel.com/docs/functions/container-images

Project name: `tripping-server`; root: `./`; preset: Other. Do not configure a static output directory.
Set `PORT=8080` in Vercel project settings so the platform routes to the application's port.
Configure the following runtime environment variables before deploying:

| Variable | Value |
| --- | --- |
| JWT_SECRET | Base64 encoding of at least 32 cryptographically random bytes; keep stable across deploys |
| FIREBASE_PROJECT_ID | Existing Firebase project ID |
| FIREBASE_SERVICE_ACCOUNT_BASE64 | Base64 encoding of the complete service account JSON, stored as a sensitive value |
| GROQ_API_KEY | Groq API key for recommendations |
| GEMINI_API_KEY | Gemini API key for code paths using Gemini |
| TOURISM_API_KEY | Tourism API key |
| KAKAO_REST_API_KEY | Kakao REST API key |
| KAKAO_CLIENT_SECRET | Kakao client secret if enabled in the Kakao console |
| KAKAO_JAVASCRIPT_KEY | Kakao JavaScript key for map rendering |
| KAKAO_REDIRECT_URI | Exact frontend OAuth callback URL registered in Kakao |
| CORS_ALLOWED_ORIGINS | Comma-separated exact frontend origins, without paths or trailing slashes |
| COURSE_MAP_ORIGIN | HTTPS origin registered for the Kakao JavaScript key |

Docker sets `COURSE_MAP_BROWSER_CHANNEL` to empty to select its installed Chromium.
Do not override it with `msedge`. Never upload Windows credential paths or enable the local profile on Vercel.
`.env.example` is a reference: Spring does not automatically load `.env` files.

After deployment, verify `/health` returns HTTP 200 and `{"status":"UP"}` without Vercel login.
Verify unauthenticated `/api/saved-courses` returns 401, then test Kakao login, recommendations,
course/map generation, save, and retrieval through the actual app.
If Vercel Deployment Protection requires a Vercel login, configure production access so app users can reach the API.

Saved-course cleanup is currently an in-process scheduler. Scale-to-zero pauses it;
physical deletion is therefore best-effort while instances are running, not guaranteed on time.
The service already filters expired courses on reads. Use Firestore TTL or an external scheduler
if timely deletion is required. Do not interpret a passing `/health` as verification of external APIs.

## Google Cloud fallback

The same Dockerfile can be built using `docker build -f Dockerfile.vercel -t tripping-server .`.
Cloud Run can use port 8080. Set FIREBASE_PROJECT_ID and give its runtime service account the
necessary Firestore permissions; omit the credential file/base64 variables to use Application Default Credentials.
Actual deployment requires a Google Cloud project and billing configuration.

## Local configuration

Local/secret/prod property files and service-account files are excluded from the built JAR.
For local runs, supply environment variables or explicitly import an external local configuration file.
JWT_SECRET is required; there is no shared default signing key.
